package com.zoritism.wirelesslinks.foundation.utility;

import net.minecraft.network.chat.Component;

public class CreateLang {
    public static Component translateDirect(String key, Object... args) {
        // Просто возвращает ключ, для теста
        return Component.literal(key);
    }
}