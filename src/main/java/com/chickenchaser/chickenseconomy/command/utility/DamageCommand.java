package com.chickenchaser.chickenseconomy.command.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DamageCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("damageitem")
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                        .executes(context -> {
                            Player player = context.getSource().getPlayerOrException();
                            ItemStack item = player.getMainHandItem();

                            if (!(item == ItemStack.EMPTY)) {
                                if (!item.isDamageableItem()) {
                                    player.sendSystemMessage(Component.literal("This item cannot be damaged").withStyle(ChatFormatting.RED));
                                } else {
                                    item.setDamageValue(IntegerArgumentType.getInteger(context, "amount"));
                                }
                            } else {
                                player.sendSystemMessage(Component.literal("You must hold the item you want damaged in your hand").withStyle(ChatFormatting.RED));
                            }
                            return 1;
                        })));
    }
}
