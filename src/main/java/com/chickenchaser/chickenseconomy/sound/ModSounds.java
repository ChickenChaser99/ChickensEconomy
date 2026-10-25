package com.chickenchaser.chickenseconomy.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    // Create the DeferredRegister for SoundEvents
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "chickenseconomy");

    // Register your specific custom song
    public static final RegistryObject<SoundEvent> PIRATE_DISC = SOUND_EVENTS.register("pirate_disc",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("chickenseconomy", "pirate_disc")));

    // Don't forget to call this in your main mod class!
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}