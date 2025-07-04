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
                mc.options.keyDown,   // S (теперь здесь S вместо A)
                mc.options.keyLeft,   // A (теперь здесь A вместо S)
                mc.options.keyRight,  // D
                mc.options.keyJump,   // Space
                mc.options.keyShift   // Shift
        ));
    }
}
