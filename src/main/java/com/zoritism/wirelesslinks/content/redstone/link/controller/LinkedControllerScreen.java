package com.zoritism.wirelesslinks.content.redstone.link.controller;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.List;

public class LinkedControllerScreen extends AbstractContainerScreen<LinkedControllerMenu> {

	private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

	private Button resetButton;
	private Button confirmButton;
	private List<Rect2i> extraAreas = Collections.emptyList();

	public LinkedControllerScreen(LinkedControllerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos;
		int y = topPos;

		resetButton = Button.builder(Component.literal("Reset"), button -> {
			menu.clearContents();
			menu.sendClearPacket();
		}).bounds(x + 8, y + imageHeight - 25, 60, 20).build();

		confirmButton = Button.builder(Component.literal("OK"), button -> {
			if (minecraft != null && minecraft.player != null)
				minecraft.player.closeContainer();
		}).bounds(x + imageWidth - 68, y + imageHeight - 25, 60, 20).build();

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, 8, 6, 0x404040, false);
		graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderTooltip(graphics, mouseX, mouseY);
	}

	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
