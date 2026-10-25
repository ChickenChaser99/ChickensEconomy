package com.chickenchaser.chickenseconomy;

import com.chickenchaser.chickenseconomy.client.TradeScreen;
import com.chickenchaser.chickenseconomy.command.economy.*;
import com.chickenchaser.chickenseconomy.command.utility.DamageCommand;
import com.chickenchaser.chickenseconomy.item.ModItems;
import com.chickenchaser.chickenseconomy.menu.ModMenuTypes;
import com.chickenchaser.chickenseconomy.network.ModNetworking;
import com.chickenchaser.chickenseconomy.sound.ModSounds;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "chickenseconomy";

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register registries (DeferredRegisters)
        ModMenuTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);

        // Mod lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Register to game event bus (commands, entity events, etc.)
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.TRADE_MENU.get(), TradeScreen::new));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
        ShopCommand.register(event.getDispatcher(), event.getBuildContext());
        ServerShopCommand.register(event.getDispatcher(), event.getBuildContext());
        TradeCommand.register(event.getDispatcher());
        RepairCommand.register(event.getDispatcher());
        DamageCommand.register(event.getDispatcher());
        ChipExchangeCommand.register(event.getDispatcher());
        DisenchantCommand.register(event.getDispatcher(), event.getBuildContext());
    };



}