package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public enum AllGuiTextures {
    // Пример — как в оригинальном Create
    PLAYER_INVENTORY("minecraft", "container/inventory", 0, 0, 176, 108),
    // Используй curiosities_2.png для контроллера, координаты и размеры подбирай под свой арт
    LINKED_CONTROLLER("wirelesslinks", "curiosities_2", 0, 0, 179, 109),
    // Добавляй свои элементы по аналогии с Create
    // ПРИМЕР: SOME_WIDGET("wirelesslinks", "widgets", 32, 16, 24, 24),

    ;

    private final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    AllGuiTextures(String namespace, String sheet, int width, int height) {
        this(namespace, sheet, 0, 0, width, height);
    }

    AllGuiTextures(String namespace, String sheet, int startX, int startY, int width, int height) {
        this.location = new ResourceLocation(namespace, "textures/gui/" + sheet + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }
}