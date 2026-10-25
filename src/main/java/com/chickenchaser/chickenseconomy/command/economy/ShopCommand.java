package com.chickenchaser.chickenseconomy.command.economy;

import com.chickenchaser.chickenseconomy.shop.ShopEvents;
import com.chickenchaser.chickenseconomy.shop.ShopSavedData.ShopMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public class ShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("chickenshop")
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("buy");
                            builder.suggest("sell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                                .executes(ctx -> startSetup(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        ItemArgument.getItem(ctx, "item"),
                                                        IntegerArgumentType.getInteger(ctx, "quantity"),
                                                        LongArgumentType.getLong(ctx, "price"))))))));
    }

    private static int startSetup(ServerPlayer player, String modeStr, ItemInput itemInput, int qty, long price) {
        ShopMode mode;
        try {
            mode = ShopMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("Mode must be 'buy' or 'sell'!").withStyle(ChatFormatting.RED));
            return 0;
        }

        Item item = itemInput.getItem();
        ShopEvents.queuePendingShop(player.getUUID(), mode, item, qty, price, false);

        player.sendSystemMessage(Component.literal("Shop configured: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(mode.name() + " " + qty + "x " + item.getDescription().getString() + " for $" + price).withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal("Now right-click the chest you want to bind this shop to.")
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }
}