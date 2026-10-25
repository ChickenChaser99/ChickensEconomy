package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import com.chickenchaser.chickenseconomy.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class ChipExchangeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chip")
                .executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal("Use /chip [buy/sell] to exchange your money for chips.").withStyle(ChatFormatting.RED));
                    return 1;
                })
                .then(Commands.literal("buy")
                        .then(Commands.argument("amount", LongArgumentType.longArg())
                                .executes(context -> {
                                    Player player = context.getSource().getPlayerOrException();
                                    UUID playerUuid = player.getUUID();
                                    BankSavedData bank = BankSavedData.get(context.getSource().getLevel());
                                    Long totalChips = LongArgumentType.getLong(context, "amount");
                                    Long price = totalChips * 25;

                                    if (bank.modifyBalance(playerUuid, -price)){
                                        ItemStack items = new ItemStack(ModItems.CHIP.get(), Math.toIntExact(totalChips));
                                        if (!player.getInventory().add(items) || !items.isEmpty()){
                                            ItemEntity droppedItem = player.drop(items, false);
                                            if (droppedItem == null){
                                                droppedItem.setNoPickUpDelay();
                                                droppedItem.setTarget(playerUuid);
                                            }
                                        }
                                        player.sendSystemMessage(Component.literal("Bought " + totalChips + " for $" + price).withStyle(ChatFormatting.GREEN));
                                    } else {
                                        player.sendSystemMessage(Component.literal("You have insufficient funds").withStyle(ChatFormatting.RED));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("sell")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(context -> {
                                    Player player = context.getSource().getPlayerOrException();
                                    UUID playerUuid = player.getUUID();
                                    Item chipItem = ModItems.CHIP.get();
                                    int chipCount = player.getInventory().countItem(chipItem);
                                    int saleAmount = IntegerArgumentType.getInteger(context, "amount");
                                    long saleValue = saleAmount * 25L;
                                    BankSavedData bank = BankSavedData.get(context.getSource().getLevel());

                                    if (chipCount >= saleAmount) {
                                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                            ItemStack stack = player.getInventory().getItem(i);

                                            if (stack.getItem() == chipItem) {
                                                // Figure out how many to take from this specific stack
                                                int take = Math.min(saleAmount, stack.getCount());

                                                // Shrink the stack (Vanilla automatically deletes the stack if it hits 0)
                                                stack.shrink(take);

                                                // Reduce our remaining target amount
                                                saleAmount -= take;

                                                // Stop looping if we've collected enough
                                                if (saleAmount <= 0) {
                                                    break;
                                                }
                                            }

                                        }
                                        bank.modifyBalance(playerUuid, saleValue);
                                        player.sendSystemMessage(Component.literal("Successfully sold $" + saleValue + " worth of chips.").withStyle(ChatFormatting.GREEN));

                                    } else {
                                            player.sendSystemMessage(Component.literal("You do not have enough chips.").withStyle(ChatFormatting.RED));
                                        }
                                    return 1;
                                })
                        )
                )
        );
}
}
