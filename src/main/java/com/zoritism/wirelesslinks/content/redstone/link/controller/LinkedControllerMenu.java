package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Меню контроллера — раскладка и поведение как в Create.
 * Ghost-слоты (частоты кнопок), запрет любых перемещений между ghost-слотами и обычным инвентарём.
 */
public class LinkedControllerMenu extends AbstractContainerMenu {

	private final ItemStack contentHolder;
	private final ItemStackHandler ghostInventory;
	protected final Inventory playerInventory;

	// В Create стандарт: 12 ghost-слотов (3 строки по 4, W/A/S/D/Shift/Space/и т.д.)
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
		// --- Ghost-слоты: 3 строки x 4 колонки (примерные координаты, подгоняйте под свой фон)
		int baseX = 44, baseY = 26;
		int slot = 0;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 4; col++) {
				int x = baseX + col * 22;
				int y = baseY + row * 22;
				this.addSlot(new GhostSlot(ghostInventory, slot++, x, y));
			}
		}

		// --- Инвентарь игрока (оставим стандартный нижний блок 3x9 + хотбар)
		int invY = 104;
		for (int row = 0; row < 3; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invY + row * 18));

		for (int k = 0; k < 9; ++k)
			this.addSlot(new Slot(playerInventory, k, 8 + k * 18, invY + 58));
	}

	// Ghost-слот: нельзя забирать, можно только класть (и только "фантомно", не уходит из инвентаря игрока)
	public static class GhostSlot extends SlotItemHandler {
		public GhostSlot(ItemStackHandler handler, int idx, int x, int y) {
			super(handler, idx, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return true;
		}
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		// Запретить "выброс" контроллера через Q/выкидывание из меню
		if (slotId == playerInventory.selected && clickTypeIn != ClickType.THROW)
			return;
		// Ghost-слоты: заменять предметы только фантомно (как в Create)
		if (slotId >= 0 && slotId < SLOT_COUNT) {
			Slot slot = slots.get(slotId);
			if (slot instanceof GhostSlot) {
				ItemStack held = player.containerMenu.getCarried();
				if (held.isEmpty()) {
					slot.set(ItemStack.EMPTY);
				} else {
					ItemStack ghostCopy = held.copy();
					ghostCopy.setCount(1);
					slot.set(ghostCopy);
				}
				// Не вызываем super.clicked — исключаем дефолтное поведение!
				return;
			}
		}
		super.clicked(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return player.getMainHandItem() == contentHolder;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		// Нет автоматического перемещения (shift-click)
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

	public void sendClearPacket() {
		// TODO: Реализовать отправку на сервер (если кнопка Reset на клиенте)
	}
}