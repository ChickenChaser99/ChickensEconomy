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

public class ServerShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("chickenservershop")
                .requires(source -> source.hasPermission(2)) // Operator / Admin only
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
        // Set isServerShop = true
        ShopEvents.queuePendingShop(player.getUUID(), mode, item, qty, price, true);

        player.sendSystemMessage(Component.literal("[ADMIN] Server Shop configured: ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(mode.name() + " " + qty + "x " + item.getDescription().getString() + " for $" + price).withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.literal("Right-click a chest to bind this infinite server shop.").withStyle(ChatFormatting.AQUA));
        return 1;
    }
}