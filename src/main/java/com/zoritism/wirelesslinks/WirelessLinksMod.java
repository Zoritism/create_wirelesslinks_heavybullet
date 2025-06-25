package com.zoritism.wirelesslinks;

import com.zoritism.wirelesslinks.client.ClientInit;
import com.zoritism.wirelesslinks.foundation.network.ModPackets;
import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import com.zoritism.wirelesslinks.registry.ModBlocks;
import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.registry.ModMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(WirelessLinksMod.MODID)
public class WirelessLinksMod {
    public static final String MODID = "wirelesslinks";
    private static final Logger LOGGER = LogManager.getLogger();

    public WirelessLinksMod() {
        LOGGER.info("[WIRELESSLINKS] Mod constructor started.");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрация всех компонентов
        ModBlocks.register(modBus);
        LOGGER.info("[WIRELESSLINKS] ModBlocks registered.");
        ModItems.ITEMS.register(modBus);
        LOGGER.info("[WIRELESSLINKS] ModItems registered.");
        ModBlockEntities.register(modBus);
        LOGGER.info("[WIRELESSLINKS] ModBlockEntities registered.");
        ModMenus.MENUS.register(modBus);
        LOGGER.info("[WIRELESSLINKS] ModMenus registered.");

        // Подписка на глобальные события (в том числе креативные вкладки)
        modBus.addListener(ModBlocks::onCreativeTabBuild);
        LOGGER.info("[WIRELESSLINKS] Creative tab build listener registered.");

        // Регистрация сетевых пакетов в момент common setup
        modBus.addListener(this::onCommonSetup);
        LOGGER.info("[WIRELESSLINKS] onCommonSetup listener registered.");

        // Общие события Minecraft
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("[WIRELESSLINKS] MinecraftForge event bus registered.");

        // Клиентская инициализация
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
        LOGGER.info("[WIRELESSLINKS] ClientInit registered (if client).");

        LOGGER.info("[WIRELESSLINKS] Mod constructor finished.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[WIRELESSLINKS] onCommonSetup called, registering packets...");
        event.enqueueWork(() -> {
            LOGGER.info("[WIRELESSLINKS] Calling ModPackets.registerPackets()");
            ModPackets.registerPackets();
        });
    }
}