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

@Mod(WirelessLinksMod.MODID)
public class WirelessLinksMod {
    public static final String MODID = "wirelesslinks";

    public WirelessLinksMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрация всех компонентов
        ModBlocks.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.MENUS.register(modBus);

        // Подписка на глобальные события (в том числе креативные вкладки)
        modBus.addListener(ModBlocks::onCreativeTabBuild);

        // Регистрация сетевых пакетов в момент common setup
        modBus.addListener(this::onCommonSetup);

        // Общие события Minecraft
        MinecraftForge.EVENT_BUS.register(this);

        // Клиентская инициализация
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPackets.registerPackets();
        });
    }
}