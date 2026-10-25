package com.chickenchaser.chickenseconomy.shop;

import com.chickenchaser.chickenseconomy.Main;
import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import com.chickenchaser.chickenseconomy.network.ModNetworking;
import com.chickenchaser.chickenseconomy.shop.ShopSavedData.ShopMode;
import com.chickenchaser.chickenseconomy.shop.ShopSavedData.ShopRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShopEvents {
    private record PendingConfig(ShopMode mode, Item item, int quantity, long price, boolean isServerShop) {}
    private static final Map<UUID, PendingConfig> PENDING = new HashMap<>();

    public static void queuePendingShop(UUID player, ShopMode mode, Item item, int qty, long price, boolean isServerShop) {
        PENDING.put(player, new PendingConfig(mode, item, qty, price, isServerShop));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof ChestBlockEntity)) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        ShopSavedData shopData = ShopSavedData.get(level);
        ShopRecord shop = shopData.getShop(pos);

        // 1. Binding pending shop configuration
        if (PENDING.containsKey(player.getUUID())) {
            PendingConfig cfg = PENDING.remove(player.getUUID());
            shopData.addShop(pos, player.getUUID(), cfg.mode(), cfg.item(), cfg.quantity(), cfg.price(), cfg.isServerShop());

            ModNetworking.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new ModNetworking.SyncShopS2CPacket(pos, cfg.mode().name(), BuiltInRegistries.ITEM.getKey(cfg.item()), cfg.quantity(), cfg.price(), cfg.isServerShop(), false));

            player.sendSystemMessage(Component.literal((cfg.isServerShop() ? "[Server Shop]" : "[Player Shop]") + " successfully bound to chest!").withStyle(ChatFormatting.GREEN));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 2. Existing Shop Interaction
        if (shop != null) {
            boolean isOwner = player.getUUID().equals(shop.owner());
            boolean isAdmin = player.hasPermissions(2);

            // Shift-click handling: Owners & Admins open chest inventory to manage stock
            if (player.isShiftKeyDown()) {
                if (shop.isServerShop() && isAdmin) {
                    return; // Admin can open server shop chest
                }
                if (!shop.isServerShop() && isOwner) {
                    return; // Owner can open their own shop chest
                }
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            // Owner normal-click feedback
            if (!shop.isServerShop() && isOwner) {
                player.sendSystemMessage(Component.literal("Shift + Right Click to open and manage your shop.").withStyle(ChatFormatting.YELLOW));
                return;
            }

            boolean isShiftClick = player.isShiftKeyDown();

            if (shop.isServerShop()) {
                processServerShopTransaction(player, level, shop, isShiftClick);
            } else {
                processPlayerShopTransaction(player, level, be, shop, isShiftClick);
            }
        }
    }

    private static void processServerShopTransaction(ServerPlayer customer, ServerLevel level, ShopRecord shop, boolean sellAll) {
        BankSavedData bank = BankSavedData.get(level);

        if (shop.mode() == ShopMode.BUY) {
            // Standard single purchase
            if (!bank.modifyBalance(customer.getUUID(), -shop.price())) {
                customer.sendSystemMessage(Component.literal("You need $" + shop.price() + " to buy this!").withStyle(ChatFormatting.RED));
                return;
            }

            ItemStack bought = new ItemStack(shop.item(), shop.quantity());
            ItemHandlerHelper.giveItemToPlayer(customer, bought);

            customer.sendSystemMessage(Component.literal("Purchased " + shop.quantity() + "x " + bought.getHoverName().getString() + " from Server for $" + shop.price())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            // Count matching items in customer inventory
            int matchingCount = countItemsInInventory(customer, shop.item());

            if (matchingCount < shop.quantity()) {
                customer.sendSystemMessage(Component.literal("You need at least " + shop.quantity() + "x " + shop.item().getDescription().getString() + " to sell!").withStyle(ChatFormatting.RED));
                return;
            }

            // Calculate batches: sell all valid units or just one unit
            int unitsToSell = sellAll ? (matchingCount / shop.quantity()) : 1;
            int totalItemsToRemove = unitsToSell * shop.quantity();
            long totalPayout = unitsToSell * shop.price();

            removeItemsFromInventory(customer, shop.item(), totalItemsToRemove);
            bank.modifyBalance(customer.getUUID(), totalPayout);

            customer.sendSystemMessage(Component.literal("Sold " + totalItemsToRemove + "x " + shop.item().getDescription().getString() + " to Server for $" + totalPayout)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static void processPlayerShopTransaction(ServerPlayer customer, ServerLevel level, BlockEntity chestBe, ShopRecord shop, boolean sellAll) {
        BankSavedData bank = BankSavedData.get(level);

        chestBe.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(chest -> {
            if (shop.mode() == ShopMode.BUY) {
                // Customer buying from player shop
                int targetSlot = -1;
                for (int i = 0; i < chest.getSlots(); i++) {
                    ItemStack st = chest.getStackInSlot(i);
                    if (st.is(shop.item()) && st.getCount() >= shop.quantity()) {
                        targetSlot = i;
                        break;
                    }
                }

                if (targetSlot == -1) {
                    customer.sendSystemMessage(Component.literal("This shop is out of stock!").withStyle(ChatFormatting.RED));
                    return;
                }

                if (!bank.modifyBalance(customer.getUUID(), -shop.price())) {
                    customer.sendSystemMessage(Component.literal("You need $" + shop.price() + " to buy this!").withStyle(ChatFormatting.RED));
                    return;
                }

                ItemStack bought = chest.extractItem(targetSlot, shop.quantity(), false);
                ItemHandlerHelper.giveItemToPlayer(customer, bought);
                bank.modifyBalance(shop.owner(), shop.price());

                customer.sendSystemMessage(Component.literal("Purchased " + shop.quantity() + "x " + bought.getHoverName().getString() + " for $" + shop.price())
                        .withStyle(ChatFormatting.GREEN));

            } else {
                // Customer selling to player shop
                int matchingCount = countItemsInInventory(customer, shop.item());

                if (matchingCount < shop.quantity()) {
                    customer.sendSystemMessage(Component.literal("You need at least " + shop.quantity() + "x " + shop.item().getDescription().getString() + " to sell!").withStyle(ChatFormatting.RED));
                    return;
                }

                long ownerBalance = bank.getBalance(shop.owner());
                if (ownerBalance < shop.price()) {
                    customer.sendSystemMessage(Component.literal("The owner cannot afford to buy your items right now!").withStyle(ChatFormatting.RED));
                    return;
                }

                int maxPossibleUnits = sellAll ? (matchingCount / shop.quantity()) : 1;

                // Restrict by owner's buying budget
                int unitsAffordable = (int) Math.min(maxPossibleUnits, ownerBalance / shop.price());
                if (unitsAffordable <= 0) {
                    customer.sendSystemMessage(Component.literal("The owner cannot afford this transaction!").withStyle(ChatFormatting.RED));
                    return;
                }

                // Check chest capacity by simulating insertions
                int unitsFit = 0;
                for (int u = 1; u <= unitsAffordable; u++) {
                    ItemStack simulated = new ItemStack(shop.item(), u * shop.quantity());
                    if (canFitInChest(chest, simulated)) {
                        unitsFit = u;
                    } else {
                        break;
                    }
                }

                if (unitsFit <= 0) {
                    customer.sendSystemMessage(Component.literal("The shop chest does not have enough room!").withStyle(ChatFormatting.RED));
                    return;
                }

                int totalItemsToTransfer = unitsFit * shop.quantity();
                long totalPayout = unitsFit * shop.price();

                // Perform actual item insertion into chest
                ItemStack toInsert = new ItemStack(shop.item(), totalItemsToTransfer);
                ItemHandlerHelper.insertItemStacked(chest, toInsert, false);

                // Remove from player inventory
                removeItemsFromInventory(customer, shop.item(), totalItemsToTransfer);

                // Exchange currency
                bank.modifyBalance(shop.owner(), -totalPayout);
                bank.modifyBalance(customer.getUUID(), totalPayout);

                customer.sendSystemMessage(Component.literal("Sold " + totalItemsToTransfer + "x " + shop.item().getDescription().getString() + " for $" + totalPayout)
                        .withStyle(ChatFormatting.GREEN));
            }
        });
    }

    private static int countItemsInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(ServerPlayer player, Item item, int totalToRemove) {
        int remaining = totalToRemove;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    private static boolean canFitInChest(IItemHandler chest, ItemStack stack) {
        ItemStack testStack = stack.copy();
        return ItemHandlerHelper.insertItemStacked(chest, testStack, true).isEmpty();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        ShopSavedData shopData = ShopSavedData.get(level);
        ShopRecord shop = shopData.getShop(pos);

        if (shop != null) {
            Player player = event.getPlayer();
            if (shop.isServerShop()) {
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.literal("Only admins can break server shops!").withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                    return;
                }
            } else if (!player.getUUID().equals(shop.owner()) && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("You cannot break someone else's shop!").withStyle(ChatFormatting.RED));
                event.setCanceled(true);
                return;
            }

            shopData.removeShop(pos);
            ModNetworking.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new ModNetworking.SyncShopS2CPacket(pos, "", BuiltInRegistries.ITEM.getKey(shop.item()), 0, 0, false, true));

            player.sendSystemMessage(Component.literal("Shop removed.").withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopSavedData data = ShopSavedData.get(player.serverLevel());
            data.getShops().forEach((pos, record) -> {
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ModNetworking.SyncShopS2CPacket(pos, record.mode().name(), BuiltInRegistries.ITEM.getKey(record.item()), record.quantity(), record.price(), record.isServerShop(), false));
            });
        }
    }
}