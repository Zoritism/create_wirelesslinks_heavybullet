package com.zoritism.wirelesslinks.content.redstone.link.controller;

import static com.zoritism.wirelesslinks.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.zoritism.wirelesslinks.foundation.gui.AllGuiTextures;
import com.zoritism.wirelesslinks.foundation.gui.AllIcons;
import com.zoritism.wirelesslinks.foundation.gui.menu.AbstractSimiContainerScreen;
import com.zoritism.wirelesslinks.foundation.gui.widget.IconButton;
import com.zoritism.wirelesslinks.foundation.utility.ControlsUtil;
import com.zoritism.wirelesslinks.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Корректная реализация: полностью скрывает стандартное число в ghost-слотах,
 * показывая его только при зажатом ctrl и если ctrl был отпущен после открытия.
 */
public class LinkedControllerScreen extends AbstractSimiContainerScreen<LinkedControllerMenu> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	private static final int GHOST_SLOTS = 12;

	// Для логики "ctrlReleasedSinceOpen"
	private boolean ctrlReleasedSinceOpen = false;

	public LinkedControllerScreen(LinkedControllerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.background = AllGuiTextures.LINKED_CONTROLLER;
	}

	@Override
	protected void init() {
		setWindowSize(background.getWidth(), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(1, 0);
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.getWidth() - 62, y + background.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			menu.saveData(menu.contentHolder);
			menu.sendClearPacket();
		});
		confirmButton = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			if (minecraft != null && minecraft.player != null)
				minecraft.player.closeContainer();
		});

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + background.getWidth() + 4, y + background.getHeight() - 44, 64, 56));

		// Сброс состояния по ctrl на момент открытия
		ctrlReleasedSinceOpen = false;
	}

	/**
	 * Подменяет count у ghostInventory только для рендера.
	 */
	private ItemStack[] backupGhostStacks = new ItemStack[GHOST_SLOTS];

	private void patchGhostInventoryCountsForRender(boolean showCounts) {
		ItemStackHandler ghost = menu.ghostInventory;
		for (int i = 0; i < GHOST_SLOTS; i++) {
			ItemStack orig = ghost.getStackInSlot(i);
			backupGhostStacks[i] = orig.copy(); // backup!
			if (!orig.isEmpty()) {
				int needCount = showCounts ? backupGhostStacks[i].getCount() : 1;
				if (orig.getCount() != needCount) {
					ItemStack patched = orig.copy();
					patched.setCount(needCount);
					ghost.setStackInSlot(i, patched);
				}
			}
		}
	}

	private void restoreGhostInventoryCountsAfterRender() {
		ItemStackHandler ghost = menu.ghostInventory;
		for (int i = 0; i < GHOST_SLOTS; i++) {
			ItemStack current = ghost.getStackInSlot(i);
			ItemStack backup = backupGhostStacks[i];
			// Только если реально разные — иначе лишние события
			if (!ItemStack.isSameItemSameTags(current, backup) || current.getCount() != backup.getCount()) {
				ghost.setStackInSlot(i, backup.copy());
			}
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		boolean ctrlDown = Screen.hasControlDown();

		// Фикс: только если ctrl был отпущен после открытия и снова нажат -- показываем числа
		if (!ctrlDown) {
			ctrlReleasedSinceOpen = true;
		}
		boolean showCtrlNumbers = ctrlReleasedSinceOpen && ctrlDown;

		patchGhostInventoryCountsForRender(showCtrlNumbers);
		try {
			super.render(graphics, mouseX, mouseY, partialTicks);
			if (showCtrlNumbers) {
				renderGhostSlotCounts(graphics, mouseX, mouseY);
			}
		} finally {
			restoreGhostInventoryCountsAfterRender();
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.getHeight() + 4;

		PLAYER_INVENTORY.render(graphics, invX, invY);
		renderPlayerInventory(graphics, invX, invY);

		int x = leftPos;
		int y = topPos;

		background.render(graphics, x, y);
		graphics.drawString(font, title, x + 15, y + 4, 0x592424, false);

		// Если реализуете GuiGameElement — раскомментируйте этот блок:
		// GuiGameElement.of(menu.contentHolder).<GuiGameElement.GuiRenderBuilder>at(x + background.getWidth() - 4, y + background.getHeight() - 56, -200)
		//     .scale(5)
		//     .render(graphics);
	}

	/**
	 * Кастомный рендер количества предметов в ghost-слотах (только если showCtrlNumbers).
	 */
	protected void renderGhostSlotCounts(GuiGraphics graphics, int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getInstance();
		ItemStackHandler ghost = menu.ghostInventory;
		for (int i = 0; i < GHOST_SLOTS; i++) {
			ItemStack stack = ghost.getStackInSlot(i);
			if (!stack.isEmpty() && backupGhostStacks[i].getCount() > 1) {
				Slot slot = null;
				for (Slot s : menu.slots) {
					if (s.container == ghost && s.getSlotIndex() == i) {
						slot = s;
						break;
					}
				}
				if (slot != null) {
					int xPos = slot.x + leftPos;
					int yPos = slot.y + topPos;
					String count = String.valueOf(backupGhostStacks[i].getCount());
					graphics.drawString(mc.font, count, xPos + 17 - mc.font.width(count), yPos + 9, 0xFFFFFF, true);
				}
			}
		}
	}

	@Override
	protected void containerTick() {
		if (minecraft != null && minecraft.player != null &&
				!minecraft.player.getMainHandItem().equals(menu.contentHolder, false))
			minecraft.player.closeContainer();

		super.containerTick();
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int x, int y) {
		if (!menu.getCarried().isEmpty() || this.hoveredSlot == null || hoveredSlot.container == menu.playerInventory) {
			super.renderTooltip(graphics, x, y);
			return;
		}

		List<Component> list = new LinkedList<>();
		if (hoveredSlot.hasItem())
			list = getTooltipFromContainerItem(hoveredSlot.getItem());

		graphics.renderComponentTooltip(font, addToTooltip(list, hoveredSlot.getSlotIndex()), x, y);
	}

	private List<Component> addToTooltip(List<Component> list, int slot) {
		if (slot < 0 || slot >= 12)
			return list;
		String key = ControlsUtil.getControls().get(slot / 2);
		String keyName = Component.translatable(key).getString();
		list.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + ((slot % 2) + 1), keyName)
				.copy().withStyle(ChatFormatting.GOLD));
		return list;
	}

	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}