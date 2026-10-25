package com.chickenchaser.chickenseconomy.network;

import com.chickenchaser.chickenseconomy.Main;
import com.chickenchaser.chickenseconomy.client.ClientShopData;
import com.chickenchaser.chickenseconomy.menu.TradeMenu;
import com.chickenchaser.chickenseconomy.shop.ShopSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Main.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, UpdateMoneyOfferC2SPacket.class,
                UpdateMoneyOfferC2SPacket::encode,
                UpdateMoneyOfferC2SPacket::new,
                UpdateMoneyOfferC2SPacket::handle);

        CHANNEL.registerMessage(packetId++, ToggleReadyC2SPacket.class,
                ToggleReadyC2SPacket::encode,
                ToggleReadyC2SPacket::new,
                ToggleReadyC2SPacket::handle);

        CHANNEL.registerMessage(packetId++, SyncShopS2CPacket.class,
                SyncShopS2CPacket::encode,
                SyncShopS2CPacket::new,
                SyncShopS2CPacket::handle);
    }

    public record UpdateMoneyOfferC2SPacket(long amount) {
        public UpdateMoneyOfferC2SPacket(FriendlyByteBuf buf) {
            this(buf.readLong());
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeLong(amount);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.containerMenu instanceof TradeMenu menu) {
                    menu.setMoneyOffer(player, Math.max(0, amount));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ToggleReadyC2SPacket {
        public ToggleReadyC2SPacket() {}

        public ToggleReadyC2SPacket(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.containerMenu instanceof TradeMenu menu) {
                    menu.toggleReady(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record SyncShopS2CPacket(BlockPos pos, String mode, ResourceLocation itemLoc, int quantity, long price, boolean isServer, boolean remove) {
        public SyncShopS2CPacket(FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readUtf(), buf.readResourceLocation(), buf.readInt(), buf.readLong(), buf.readBoolean(), buf.readBoolean());
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(mode);
            buf.writeResourceLocation(itemLoc);
            buf.writeInt(quantity);
            buf.writeLong(price);
            buf.writeBoolean(isServer);
            buf.writeBoolean(remove);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (remove) {
                    ClientShopData.remove(pos);
                } else {
                    ClientShopData.put(pos, ShopSavedData.ShopMode.valueOf(mode), itemLoc, quantity, price, isServer);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}

