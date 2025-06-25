package com.zoritism.wirelesslinks.foundation.animation;

import net.minecraft.client.Minecraft;

public class AnimationTickHolder {
    public static float getPartialTicks() {
        return Minecraft.getInstance().getPartialTick();
    }

    public static long getTicks() {
        return Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : 0L;
    }

    public static float getRenderTime() {
        return getTicks() + getPartialTicks();
    }
}
