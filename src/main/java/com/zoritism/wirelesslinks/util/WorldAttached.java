package com.zoritism.wirelesslinks.util;

import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Простая замена WorldAttached из Create.
 * Хранит объект по миру.
 */
public class WorldAttached<T> {

    private final Map<LevelAccessor, T> storage = new HashMap<>();
    private final Function<LevelAccessor, T> factory;

    public WorldAttached(Function<LevelAccessor, T> factory) {
        this.factory = factory;
    }

    public T get(LevelAccessor level) {
        return storage.computeIfAbsent(level, factory);
    }

    public void clear(LevelAccessor level) {
        storage.remove(level);
    }
}
