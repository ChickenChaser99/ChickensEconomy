package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.economy.BankSavedData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /balance (or /bal)
        dispatcher.register(Commands.literal("balance")
                .executes(ctx -> showBalance(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(src -> src.hasPermission(2)) // OP check to see others
                        .executes(ctx -> showBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        dispatcher.register(Commands.literal("bal")
                .executes(ctx -> showBalance(ctx.getSource(), ctx.getSource().getPlayerOrException())));

        // /pay <player> <amount>
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> transferFunds(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "target"),
                                        LongArgumentType.getLong(ctx, "amount"))))));

        dispatcher.register(Commands.literal("serverpay")
                .requires(source -> source.hasPermission(2)) // Check permission at the root literal
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> createFunds(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target"),
                                        LongArgumentType.getLong(ctx, "amount")

                                ))
                        )
                )
        );
    }

    private static int showBalance(CommandSourceStack source, ServerPlayer target) {
        BankSavedData bank = BankSavedData.get(target.serverLevel());
        long balance = bank.getBalance(target.getUUID());
        source.sendSuccess(() -> Component.literal(target.getScoreboardName() + "'s balance: ")
                .append(Component.literal("$" + balance).withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    private static int transferFunds(ServerPlayer sender, ServerPlayer receiver, long amount) {
        if (sender.getUUID().equals(receiver.getUUID())) {
            sender.sendSystemMessage(Component.literal("You cannot pay yourself!").withStyle(ChatFormatting.RED));
            return 0;
        }

        BankSavedData bank = BankSavedData.get(sender.serverLevel());
        if (!bank.modifyBalance(sender.getUUID(), -amount)) {
            sender.sendSystemMessage(Component.literal("Insufficient funds!").withStyle(ChatFormatting.RED));
            return 0;
        }

        bank.modifyBalance(receiver.getUUID(), amount);
        sender.sendSystemMessage(Component.literal("Sent $" + amount + " to " + receiver.getScoreboardName()).withStyle(ChatFormatting.GREEN));
        receiver.sendSystemMessage(Component.literal("Received $" + amount + " from " + sender.getScoreboardName()).withStyle(ChatFormatting.GOLD));
        return 1;
    }
    private static int createFunds(CommandSourceStack source, ServerPlayer target, long amount) {
        BankSavedData bank = BankSavedData.get(target.serverLevel());
        bank.modifyBalance(target.getUUID(), amount);
        target.sendSystemMessage(Component.literal("$" + amount + " has been added to your account.").withStyle(ChatFormatting.GREEN));

        source.sendSuccess(
                () -> Component.literal("Added $" + amount + " to " + target.getName().getString() + "'s balance."),
                true

        );

        return Command.SINGLE_SUCCESS; // Returns 1 for success
    }
}