package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public enum AllGuiTextures {
    // Важно: путь должен быть "textures/gui/player_inventory.png" (без namespace!)
    PLAYER_INVENTORY("player_inventory", 176, 108),
    // Контроллер: подбери размеры и координаты под свою текстуру
    LINKED_CONTROLLER("curiosities_2", 179, 109);

    private final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    AllGuiTextures(String sheet, int width, int height) {
        this(sheet, 0, 0, width, height);
    }

    AllGuiTextures(String sheet, int startX, int startY, int width, int height) {
        this.location = new ResourceLocation("wirelesslinks", "textures/gui/" + sheet + ".png");
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