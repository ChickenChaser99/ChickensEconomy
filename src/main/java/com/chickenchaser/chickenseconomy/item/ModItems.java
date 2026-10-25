package com.chickenchaser.chickenseconomy.item;


import com.chickenchaser.chickenseconomy.sound.ModSounds;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "chickenseconomy");

    public static final RegistryObject<Item> CHIP = ITEMS.register("chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PIRATE_DISC = ITEMS.register("pirate_disc",
            () -> new RecordItem(
                    15, // Comparator signal strength (1-15)
                    ModSounds.PIRATE_DISC, // The sound we registered in Step 1
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE), // Max stack 1, yellow text
                    1800 // The length of your song in ticks (e.g., 2 minutes = 2400 ticks)
            ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

