package com.zoritism.wirelesslinks.registry;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlock;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LecternControllerBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WirelessLinksMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WirelessLinksMod.MODID);

    public static final RegistryObject<Block> REDSTONE_LINK = BLOCKS.register(
            "redstone_link", RedstoneLinkBlock::new);

    public static final RegistryObject<Item> REDSTONE_LINK_ITEM = ITEMS.register(
            "redstone_link", () -> new BlockItem(REDSTONE_LINK.get(), new Item.Properties()));

    // --- Регистрация лекторна для контроллера ---
    public static final RegistryObject<Block> LECTERN_CONTROLLER = BLOCKS.register(
            "lectern_controller", () -> new LecternControllerBlock(Block.Properties.of().strength(1.5f).noOcclusion()));

    public static final RegistryObject<Item> LECTERN_CONTROLLER_ITEM = ITEMS.register(
            "lectern_controller", () -> new BlockItem(LECTERN_CONTROLLER.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static void onCreativeTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(REDSTONE_LINK_ITEM.get());
            event.accept(LECTERN_CONTROLLER_ITEM.get());
        }
    }
}