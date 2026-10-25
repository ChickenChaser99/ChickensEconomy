package com.chickenchaser.chickenseconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopSavedData extends SavedData {
    private static final String DATA_NAME = "chickens_chest_shops";

    public enum ShopMode { BUY, SELL }

    // Added boolean isServerShop
    public record ShopRecord(UUID owner, ShopMode mode, Item item, int quantity, long price, boolean isServerShop) {}

    private final Map<BlockPos, ShopRecord> shops = new HashMap<>();

    public static ShopSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ShopSavedData::load, ShopSavedData::new, DATA_NAME);
    }

    public void addShop(BlockPos pos, UUID owner, ShopMode mode, Item item, int quantity, long price, boolean isServerShop) {
        shops.put(pos, new ShopRecord(owner, mode, item, quantity, price, isServerShop));
        setDirty();
    }

    public ShopRecord getShop(BlockPos pos) {
        return shops.get(pos);
    }

    public void removeShop(BlockPos pos) {
        if (shops.remove(pos) != null) {
            setDirty();
        }
    }

    public Map<BlockPos, ShopRecord> getShops() {
        return Collections.unmodifiableMap(shops);
    }

    public static ShopSavedData load(CompoundTag tag) {
        ShopSavedData data = new ShopSavedData();
        ListTag list = tag.getList("Shops", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            UUID owner = entry.getUUID("Owner");
            ShopMode mode = ShopMode.valueOf(entry.getString("Mode"));
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getString("Item")));
            int qty = entry.getInt("Qty");
            long price = entry.getLong("Price");
            boolean isServer = entry.getBoolean("ServerShop");
            data.shops.put(pos, new ShopRecord(owner, mode, item, qty, price, isServer));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag list = new ListTag();
        shops.forEach((pos, record) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putUUID("Owner", record.owner());
            entry.putString("Mode", record.mode().name());
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(record.item()).toString());
            entry.putInt("Qty", record.quantity());
            entry.putLong("Price", record.price());
            entry.putBoolean("ServerShop", record.isServerShop());
            list.add(entry);
        });
        compoundTag.put("Shops", list);
        return compoundTag;
    }
}