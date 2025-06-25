package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class LinkedControllerMenu extends AbstractContainerMenu {

	private final ItemStack contentHolder;
	private final ItemStackHandler ghostInventory;
	protected final Inventory playerInventory;

	public static final int SLOT_COUNT = 12;

	public LinkedControllerMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		this(type, id, inv, extraData.readItem());
	}

	public LinkedControllerMenu(MenuType<?> type, int id, Inventory inv, ItemStack heldItem) {
		super(type, id);
		this.playerInventory = inv;
		this.contentHolder = heldItem;
		this.ghostInventory = LinkedControllerItem.getFrequencyInventory(heldItem);
		this.addSlots();
	}

	public static LinkedControllerMenu create(int id, Inventory inv, ItemStack heldItem) {
		return new LinkedControllerMenu(ModMenus.LINKED_CONTROLLER_MENU.get(), id, inv, heldItem);
	}

	private void addSlots() {
		int x = 12;
		int y = 34;
		int slot = 0;

		// Ghost-слоты (частоты)
		for (int column = 0; column < 6; column++) {
			for (int row = 0; row < 2; ++row)
				this.addSlot(new SlotItemHandler(ghostInventory, slot++, x, y + row * 18) {
					@Override
					public boolean mayPickup(Player player) {
						return false; // Ghost-слот, нельзя забирать
					}

					@Override
					public boolean mayPlace(ItemStack stack) {
						return true; // Можно класть любые предметы
					}
				});
			x += 24;
			if (column == 3)
				x += 11;
		}

		// Инвентарь игрока
		for (int i = 0; i < 3; ++i)
			for (int j = 0; j < 9; ++j)
				this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 131 + i * 18));

		for (int k = 0; k < 9; ++k)
			this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 131 + 58));
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (slotId == playerInventory.selected && clickTypeIn != ClickType.THROW)
			return;
		super.clicked(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return player.getMainHandItem() == contentHolder;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		// Нет автоматического перемещения
		return ItemStack.EMPTY;
	}

	public ItemStackHandler getInventory() {
		return ghostInventory;
	}

	public ItemStack getContentHolder() {
		return contentHolder;
	}

	public void clearContents() {
		for (int i = 0; i < ghostInventory.getSlots(); i++) {
			ghostInventory.setStackInSlot(i, ItemStack.EMPTY);
		}
		// Сохраняем обратно
		LinkedControllerItem.saveFrequencyInventory(contentHolder, ghostInventory);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		// Сохраняем инвентарь при закрытии
		LinkedControllerItem.saveFrequencyInventory(contentHolder, ghostInventory);
	}

	public LinkedControllerMenu(int id, Inventory inv, ItemStack heldItem) {
		super(ModMenus.LINKED_CONTROLLER_MENU.get(), id);
		this.playerInventory = inv;
		this.contentHolder = heldItem;
		this.ghostInventory = LinkedControllerItem.getFrequencyInventory(heldItem);
		this.addSlots();
	}


	public void sendClearPacket() {
		// TODO: Реализовать при наличии кнопки очистки и клиентского пакета
	}
}
