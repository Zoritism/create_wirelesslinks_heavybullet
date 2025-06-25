package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.resources.ResourceLocation;

/**
 * Минималистичная версия AllIcons без зависимостей на Create.
 * Добавляйте новые иконки по мере необходимости.
 */
public enum AllIcons {
    I_TRASH("zoritism", "textures/gui/icons/trash.png"),
    I_CONFIRM("zoritism", "textures/gui/icons/confirm.png");
    // Добавляйте другие иконки по аналогии:
    // I_ADD("zoritism", "textures/gui/icons/add.png"),

    public final ResourceLocation location;

    AllIcons(String namespace, String path) {
        this.location = new ResourceLocation(namespace, path);
    }

    /**
     * Нарисовать иконку на экране.
     * @param graphics GuiGraphics для рендера
     * @param x левый верхний угол
     * @param y левый верхний угол
     */
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, 0, 0, 16, 16, 16, 16);
    }
}