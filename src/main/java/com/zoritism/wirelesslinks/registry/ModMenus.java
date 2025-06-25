package com.zoritism.wirelesslinks.registry;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, WirelessLinksMod.MODID);

    public static final RegistryObject<MenuType<LinkedControllerMenu>> LINKED_CONTROLLER_MENU =
            MENUS.register("linked_controller", () ->
                    IForgeMenuType.create((id, inv, buf) ->
                            new LinkedControllerMenu(
                                    ModMenus.LINKED_CONTROLLER_MENU.get(), // Передаём MenuType первым аргументом
                                    id,
                                    inv,
                                    buf.readItem()
                            )));
}