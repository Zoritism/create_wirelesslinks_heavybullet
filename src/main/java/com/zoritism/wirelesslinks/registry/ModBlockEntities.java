package com.zoritism.wirelesslinks.registry;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlockEntity;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WirelessLinksMod.MODID);

    public static final RegistryObject<BlockEntityType<RedstoneLinkBlockEntity>> REDSTONE_LINK =
            BLOCK_ENTITIES.register("redstone_link",
                    () -> BlockEntityType.Builder.of(
                            RedstoneLinkBlockEntity::new,
                            ModBlocks.REDSTONE_LINK.get()
                    ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
