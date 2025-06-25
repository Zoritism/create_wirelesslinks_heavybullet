package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.foundation.gui.menu.GhostItemMenu;
import com.zoritism.wirelesslinks.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Полностью по образцу Create: наследование от GhostItemMenu<ItemStack>,
 * ghost-слоты, сохранение, stillValid, addSlots полностью как в оригинале.
 */
public class LinkedControllerMenu extends GhostItemMenu<ItemStack> {

	public LinkedControllerMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public LinkedControllerMenu(MenuType<?> type, int id, Inventory inv, ItemStack filterItem) {
		super(type, id, inv, filterItem);
	}

	public static LinkedControllerMenu create(int id, Inventory inv, ItemStack filterItem) {
		return new LinkedControllerMenu(ModMenus.LINKED_CONTROLLER_MENU.get(), id, inv, filterItem);
	}

	@Override
	protected ItemStack createOnClient(FriendlyByteBuf extraData) {
		return extraData.readItem();
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return LinkedControllerItem.getFrequencyInventory(contentHolder);
	}

	@Override
	protected void addSlots() {
		addPlayerSlots(8, 131);

		int x = 12;
		int y = 34;
		int slot = 0;

		for (int column = 0; column < 6; column++) {
			for (int row = 0; row < 2; ++row)
				addSlot(new SlotItemHandler(ghostInventory, slot++, x, y + row * 18));
			x += 24;
			if (column == 3)
				x += 11;
		}
	}

	@Override
	protected void saveData(ItemStack contentHolder) {
		LinkedControllerItem.saveFrequencyInventory(contentHolder, ghostInventory);
	}

	@Override
	protected boolean allowRepeats() {
		return true;
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (slotId == playerInventory.selected && clickTypeIn != ClickType.THROW)
			return;
		super.clicked(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean stillValid(Player playerIn) {
		// Как в оригинале Create: playerInventory.getSelected() == contentHolder;
		// Рекомендуется использовать equals для наилучшей совместимости:
		return playerInventory.player.getMainHandItem().equals(contentHolder, false);
	}
}