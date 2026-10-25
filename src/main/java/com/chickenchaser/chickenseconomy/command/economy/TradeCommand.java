package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.menu.TradeMenu;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeCommand {
    private static final Map<UUID, UUID> PENDING_REQUESTS = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trade")
                .then(Commands.literal("accept")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> acceptTrade(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> initiateTrade(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "target")))));
    }

    private static int initiateTrade(ServerPlayer sender, ServerPlayer target) {
        if (sender.equals(target)) {
            sender.sendSystemMessage(Component.literal("You cannot trade with yourself!").withStyle(ChatFormatting.RED));
            return 0;
        }

        PENDING_REQUESTS.put(sender.getUUID(), target.getUUID());
        sender.sendSystemMessage(Component.literal("Trade request sent to " + target.getScoreboardName()).withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.literal(sender.getScoreboardName() + " wants to trade! Run ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("/trade accept " + sender.getScoreboardName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)));
        return 1;
    }

    private static int acceptTrade(ServerPlayer target, ServerPlayer sender) {
        if (!target.getUUID().equals(PENDING_REQUESTS.get(sender.getUUID()))) {
            target.sendSystemMessage(Component.literal("No pending request from that player!").withStyle(ChatFormatting.RED));
            return 0;
        }

        PENDING_REQUESTS.remove(sender.getUUID());

        SimpleContainer hostInv = new SimpleContainer(4);
        SimpleContainer guestInv = new SimpleContainer(4);
        SimpleContainerData sharedData = new SimpleContainerData(6);

        // Open Host Menu
        NetworkHooks.openScreen(sender, new MenuProvider() {
            @Override
            public Component getDisplayName() { return Component.literal("Trading with " + target.getScoreboardName()); }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                TradeMenu m = new TradeMenu(id, inv, true, hostInv, guestInv, sharedData);
                m.bindServerPlayers(sender, target);
                return m;
            }
        }, buf -> buf.writeBoolean(true));

        // Open Guest Menu
        NetworkHooks.openScreen(target, new MenuProvider() {
            @Override
            public Component getDisplayName() { return Component.literal("Trading with " + sender.getScoreboardName()); }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                TradeMenu m = new TradeMenu(id, inv, false, hostInv, guestInv, sharedData);
                m.bindServerPlayers(sender, target);
                return m;
            }
        }, buf -> buf.writeBoolean(false));

        return 1;
    }
}