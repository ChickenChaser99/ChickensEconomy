package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import com.mojang.brigadier.CommandDispatcher;

import java.util.Map;

public class DisenchantCommand {

    // Adjust this to change how much it costs to disenchant per level of the enchantment
    private static final long COST_PER_ENCHANT_LEVEL = 250L;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("disenchant")
                .then(Commands.argument("enchant", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .executes(context -> {
                            // Cast to ServerPlayer so we can get their UUID and serverLevel for the bank
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ItemStack heldItem = player.getMainHandItem();

                            // 1. Get the enchantment from the command argument
                            Holder.Reference<Enchantment> enchantHolder = ResourceArgument.getEnchantment(context, "enchant");
                            Enchantment enchantToRemove = enchantHolder.value();

                            // 2. Validate held item
                            if (heldItem.isEmpty()) {
                                player.sendSystemMessage(Component.literal("Hold the item you want to disenchant in your main hand!")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }

                            // 3. Get current enchantments map
                            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(heldItem);

                            if (!enchantments.containsKey(enchantToRemove)) {
                                player.sendSystemMessage(Component.literal("This item does not have that enchantment.")
                                        .withStyle(ChatFormatting.RED));
                                return 0;
                            }

                            int level = enchantments.get(enchantToRemove);

                            // 4. Calculate economy cost
                            long cost = level * COST_PER_ENCHANT_LEVEL;

                            BankSavedData bank = BankSavedData.get(player.serverLevel());

                            // 5. Deduct digital currency
                            if (!bank.modifyBalance(player.getUUID(), -cost)) {
                                player.sendSystemMessage(Component.literal("Removing this enchantment costs ")
                                        .withStyle(ChatFormatting.RED)
                                        .append(Component.literal("$" + cost).withStyle(ChatFormatting.GOLD))
                                        .append(Component.literal(". You have insufficient funds!").withStyle(ChatFormatting.RED)));
                                return 0;
                            }

                            // 6. Remove from the map and apply it back to the item
                            enchantments.remove(enchantToRemove);
                            EnchantmentHelper.setEnchantments(enchantments, heldItem);

                            // 7. Play sound (Grindstone sound fits disenchanting well)
                            player.serverLevel().playSound(
                                    null,
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    SoundEvents.GRINDSTONE_USE,
                                    SoundSource.PLAYERS,
                                    0.8F,
                                    1.0F
                            );

                            // Send formatted success message
                            player.sendSystemMessage(Component.literal("Removed ")
                                    .withStyle(ChatFormatting.GREEN)
                                    // This dynamically grabs the actual translated name of the enchantment!
                                    .append(Component.translatable(enchantToRemove.getDescriptionId()).withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                                    .append(Component.literal("$" + cost).withStyle(ChatFormatting.GOLD)));

                            return 1;
                        })
                )
        );
    }
}