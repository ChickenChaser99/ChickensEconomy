package com.chickenchaser.chickenseconomy.client;

import com.chickenchaser.chickenseconomy.shop.ShopSavedData.ShopMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ClientShopData {
    public record ClientShopEntry(ShopMode mode, ItemStack displayStack, int quantity, long price, boolean isServer) {}
    private static final Map<BlockPos, ClientShopEntry> ACTIVE_SHOPS = new HashMap<>();

    public static void put(BlockPos pos, ShopMode mode, ResourceLocation itemLoc, int quantity, long price, boolean isServer) {
        Item item = BuiltInRegistries.ITEM.get(itemLoc);
        ACTIVE_SHOPS.put(pos, new ClientShopEntry(mode, new ItemStack(item), quantity, price, isServer));
    }

    public static void remove(BlockPos pos) {
        ACTIVE_SHOPS.remove(pos);
    }

    public static Map<BlockPos, ClientShopEntry> getAll() {
        return ACTIVE_SHOPS;
    }
}