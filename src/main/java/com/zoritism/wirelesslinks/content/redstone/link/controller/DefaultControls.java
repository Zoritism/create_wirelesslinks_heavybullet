package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.List;
import java.util.Vector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public class DefaultControls {
    public static Vector<KeyMapping> getControls() {
        Minecraft mc = Minecraft.getInstance();
        return new Vector<>(List.of(
                mc.options.keyUp,     // W
                mc.options.keyLeft,   // A
                mc.options.keyDown,   // S
                mc.options.keyRight,  // D
                mc.options.keyShift,  // Shift (Ctrl)
                mc.options.keyJump    // Jump (Space)
        ));
    }
}