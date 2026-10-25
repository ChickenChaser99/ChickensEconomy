package com.chickenchaser.chickenseconomy.menu;

import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.UUID;

public class TradeMenu extends AbstractContainerMenu {
    private final Player localPlayer;
    private final boolean isHost;
    private final SimpleContainer hostSlots;
    private final SimpleContainer guestSlots;
    private final ContainerData syncData;

    private ServerPlayer hostPlayer;
    private ServerPlayer guestPlayer;

    // Client-side constructor
    public TradeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readBoolean(), new SimpleContainer(4), new SimpleContainer(4), new SimpleContainerData(6));
    }

    // Server-side constructor
// Server & Client constructor in TradeMenu.java
    public TradeMenu(int containerId, Inventory playerInv, boolean isHost,
                     SimpleContainer hostSlots, SimpleContainer guestSlots, ContainerData syncData) {
        super(ModMenuTypes.TRADE_MENU.get(), containerId);
        this.localPlayer = playerInv.player;
        this.isHost = isHost;
        this.hostSlots = hostSlots;
        this.guestSlots = guestSlots;
        this.syncData = syncData;

        addDataSlots(syncData);

        // Slots 0-3: Host 2x2 Grid (Left side panel)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = col + row * 2;
                addSlot(new TradeSlot(hostSlots, index, 25 + col * 18, 22 + row * 18, isHost));
            }
        }

        // Slots 4-7: Guest 2x2 Grid (Right side panel)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = col + row * 2;
                addSlot(new TradeSlot(guestSlots, index, 119 + col * 18, 22 + row * 18, !isHost));
            }
        }

        // Standard 36 Player Inventory & Hotbar slots (Centered at the bottom)
        int invYOffset = 98;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invYOffset + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, invYOffset + 58));
        }
    }

    public void bindServerPlayers(ServerPlayer host, ServerPlayer guest) {
        this.hostPlayer = host;
        this.guestPlayer = guest;
    }

    public void setMoneyOffer(ServerPlayer player, long amount) {
        BankSavedData bank = BankSavedData.get(player.serverLevel());
        long max = bank.getBalance(player.getUUID());
        int safeVal = (int) Math.min(amount, max);

        if (player.equals(hostPlayer)) {
            syncData.set(0, safeVal);
            syncData.set(2, 0); // Reset ready states on price adjust
        } else if (player.equals(guestPlayer)) {
            syncData.set(1, safeVal);
            syncData.set(3, 0);
        }
        broadcastChanges();
    }

    public void toggleReady(ServerPlayer player) {
        if (player.equals(hostPlayer)) {
            syncData.set(2, syncData.get(2) == 1 ? 0 : 1);
        } else if (player.equals(guestPlayer)) {
            syncData.set(3, syncData.get(3) == 1 ? 0 : 1);
        }
        broadcastChanges();

        if (syncData.get(2) == 1 && syncData.get(3) == 1) {
            executeTrade();
        }
    }

    private void executeTrade() {
        if (hostPlayer == null || guestPlayer == null) return;
        BankSavedData bank = BankSavedData.get(hostPlayer.serverLevel());

        long hostMoney = syncData.get(0);
        long guestMoney = syncData.get(1);

        if (bank.getBalance(hostPlayer.getUUID()) < hostMoney || bank.getBalance(guestPlayer.getUUID()) < guestMoney) {
            syncData.set(2, 0);
            syncData.set(3, 0);
            return;
        }

        // Transfer funds
        bank.modifyBalance(hostPlayer.getUUID(), guestMoney - hostMoney);
        bank.modifyBalance(guestPlayer.getUUID(), hostMoney - guestMoney);

        // Swap item sets
        for (int i = 0; i < 4; i++) {
            ItemStack toGuest = hostSlots.removeItemNoUpdate(i);
            ItemStack toHost = guestSlots.removeItemNoUpdate(i);
            ItemHandlerHelper.giveItemToPlayer(guestPlayer, toGuest);
            ItemHandlerHelper.giveItemToPlayer(hostPlayer, toHost);
        }

        hostPlayer.closeContainer();
        guestPlayer.closeContainer();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            SimpleContainer returnInv = isHost ? hostSlots : guestSlots;
            for (int i = 0; i < returnInv.getContainerSize(); i++) {
                ItemStack stack = returnInv.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, stack);
                }
            }
        }
    }

    public long getHostMoney() { return syncData.get(0); }
    public long getGuestMoney() { return syncData.get(1); }
    public boolean isHostReady() { return syncData.get(2) == 1; }
    public boolean isGuestReady() { return syncData.get(3) == 1; }
    public boolean isHost() { return isHost; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < 8) { // From trade offers to inventory
                if (!this.moveItemStackTo(slotStack, 8, 44, true)) return ItemStack.EMPTY;
            } else { // From inventory to user's trade section
                int start = isHost ? 0 : 4;
                int end = isHost ? 4 : 8;
                if (!this.moveItemStackTo(slotStack, start, end, false)) return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static class TradeSlot extends Slot {
        private final boolean isOwner;
        public TradeSlot(SimpleContainer container, int index, int x, int y, boolean isOwner) {
            super(container, index, x, y);
            this.isOwner = isOwner;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return isOwner; }

        @Override
        public boolean mayPickup(Player player) { return isOwner; }
    }
}