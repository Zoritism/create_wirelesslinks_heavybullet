package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.resources.ResourceLocation;

public enum AllGuiTextures {
    PLAYER_INVENTORY("minecraft", "textures/gui/container/inventory.png", 176, 108),
    LINKED_CONTROLLER("zoritism", "textures/gui/linked_controller.png", 179, 109);

    public final ResourceLocation location;
    public final int width, height;

    AllGuiTextures(String namespace, String path, int width, int height) {
        this.location = new ResourceLocation(namespace, path);
        this.width = width;
        this.height = height;
    }

    public ResourceLocation getLocation() { return location; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}