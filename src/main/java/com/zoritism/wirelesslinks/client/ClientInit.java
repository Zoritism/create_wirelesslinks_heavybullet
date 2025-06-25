package com.zoritism.wirelesslinks.client;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlockEntity;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkRenderer;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerScreen;
import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import com.zoritism.wirelesslinks.registry.ModBlocks;
import com.zoritism.wirelesslinks.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = "wirelesslinks", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientInit {

    /** вызывается из мод-класса в конструкторе */
    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ► экран и рендереры можем регистрировать сразу
        modBus.addListener((RegisterClientReloadListenersEvent e) -> {
            MenuScreens.register(ModMenus.LINKED_CONTROLLER_MENU.get(), LinkedControllerScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.REDSTONE_LINK.get(), RedstoneLinkRenderer::new);
        });

        // ► а слой рендера задаём в client-setup
        modBus.addListener(ClientInit::onClientSetup);
    }

    /** обработчик FMLClientSetupEvent */
    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // сообщаем движку, что Redstone Link рендерится в слое cutout
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.REDSTONE_LINK.get(),        // ваш RegistryObject<Block>
                    RenderType.cutout()
            );
        });
    }
}
