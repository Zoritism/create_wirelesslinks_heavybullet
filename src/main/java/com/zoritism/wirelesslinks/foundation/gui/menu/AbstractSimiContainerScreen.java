package com.zoritism.wirelesslinks.foundation.gui.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public abstract class AbstractSimiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractSimiContainerScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    protected void setWindowSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    protected void setWindowOffset(int x, int y) {
        this.leftPos += x;
        this.topPos += y;
    }
    protected void renderPlayerInventory(net.minecraft.client.gui.GuiGraphics graphics, int x, int y) {
        // Можно реализовать рендер инвентаря, если потребуется
    }
    protected int getLeftOfCentered(int width) {
        return leftPos + (imageWidth - width) / 2;
    }
}