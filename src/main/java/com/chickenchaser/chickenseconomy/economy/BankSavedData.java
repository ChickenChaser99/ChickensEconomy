package com.chickenchaser.chickenseconomy.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankSavedData extends SavedData {
    private static final String DATA_NAME = "chickens_economy_bank";
    private final Map<UUID, Long> balances = new HashMap<>();

    public static BankSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(BankSavedData::load, BankSavedData::new, DATA_NAME);
    }

    public long getBalance(UUID playerUuid) {
        return balances.getOrDefault(playerUuid, 100L); // 100 starter balance
    }


    public void setBalance(UUID playerUuid, long amount) {
        balances.put(playerUuid, Math.max(0, amount));
        setDirty();
    }

    public boolean modifyBalance(UUID playerUuid, long delta) {
        long current = getBalance(playerUuid);
        if (delta < 0 && current < Math.abs(delta)) {
            return false; // Insufficient funds
        }
        setBalance(playerUuid, current + delta);
        return true;
    }

    public static BankSavedData load(CompoundTag tag) {
        BankSavedData data = new BankSavedData();
        ListTag list = tag.getList("Balances", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.balances.put(entry.getUUID("UUID"), entry.getLong("Balance"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag list = new ListTag();
        balances.forEach((uuid, bal) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            entry.putLong("Balance", bal);
            list.add(entry);
        });
        compoundTag.put("Balances", list);
        return compoundTag;
    }
}