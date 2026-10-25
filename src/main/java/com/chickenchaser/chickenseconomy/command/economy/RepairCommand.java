package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class RepairCommand {

    private static final double COST_PER_THOUSAND = 250.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repair")
                .executes(ctx -> executeRepair(ctx.getSource().getPlayerOrException())));
    }

    private static int executeRepair(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();

        // 1. Validate held item
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("Hold the damaged item you want to repair in your main hand!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!held.isDamageableItem()) {
            player.sendSystemMessage(Component.literal("This item cannot be damaged or repaired!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        int damage = held.getDamageValue();
        if (damage <= 0) {
            player.sendSystemMessage(Component.literal("This item is already at full durability!")
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        long enchantments = held.getEnchantmentTags().stream().count();
        enchantments++;
        long enchantmentsCost = 250L * enchantments;


        // 2. Calculate repair cost: $25 per 1,000 durability (minimum $1)
        long cost = Math.max(1L, (long) Math.ceil((damage * COST_PER_THOUSAND) / 1000.0) + enchantmentsCost);

        BankSavedData bank = BankSavedData.get(player.serverLevel());

        // 3. Deduct digital currency
        if (!bank.modifyBalance(player.getUUID(), -cost)) {
            player.sendSystemMessage(Component.literal("Repairing this item costs ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal("$" + cost).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (Durability needed: " + damage + "). You have insufficient funds!").withStyle(ChatFormatting.RED)));
            return 0;
        }

        // 4. Restore durability and play sound
        held.setDamageValue(0);

        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ANVIL_USE,
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );

        player.sendSystemMessage(Component.literal("Repaired ")
                .withStyle(ChatFormatting.GREEN)
                .append(held.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (+" + damage + " durability) for ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("$" + cost).withStyle(ChatFormatting.GOLD)));

        return 1;
    }
}