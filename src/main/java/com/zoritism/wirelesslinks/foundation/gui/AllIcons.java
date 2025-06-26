package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public enum AllIcons {
    I_TRASH(1, 0),
    I_CONFIRM(0, 1);
    // Добавляй другие иконки по аналогии: I_ADD(2, 0), ...

    public static final ResourceLocation ICON_ATLAS = new ResourceLocation("wirelesslinks", "textures/gui/icons.png");
    public static final int ICON_SIZE = 16; // размер иконки в px
    public static final int ATLAS_SIZE = 256; // размер всего атласа, подбери по своему png

    public final int iconX;
    public final int iconY;

    AllIcons(int x, int y) {
        this.iconX = x * ICON_SIZE;
        this.iconY = y * ICON_SIZE;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, iconX, iconY, ICON_SIZE, ICON_SIZE, ATLAS_SIZE, ATLAS_SIZE);
    }
}