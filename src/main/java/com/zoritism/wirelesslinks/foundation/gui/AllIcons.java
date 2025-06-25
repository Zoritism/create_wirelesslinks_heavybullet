package com.zoritism.wirelesslinks.foundation.gui;

import net.minecraft.resources.ResourceLocation;

public enum AllIcons {
    I_TRASH("zoritism", "textures/gui/icons/trash.png"),
    I_CONFIRM("zoritism", "textures/gui/icons/confirm.png");

    public final ResourceLocation location;

    AllIcons(String namespace, String path) {
        this.location = new ResourceLocation(namespace, path);
    }
}