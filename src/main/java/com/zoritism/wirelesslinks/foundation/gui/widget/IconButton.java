package com.zoritism.wirelesslinks.foundation.gui.widget;

import com.zoritism.wirelesslinks.foundation.gui.AllIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends Button {
    private final ResourceLocation icon;

    // Конструктор
    public IconButton(int x, int y, AllIcons iconType) {
        // 20x20 - стандартный размер, пустой текст, ничего не делает по умолчанию
        super(x, y, 20, 20, Component.empty(), b -> {});
        this.icon = iconType.location;
    }

    /**
     * Позволяет указать действие по нажатию (заменяет onPress).
     * Пример использования:
     *   button.withCallback(() -> { ... });
     */
    public IconButton withCallback(Runnable callback) {
        this.onPress = b -> callback.run();
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Сначала нарисуем стандартную кнопку:
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        // Потом нарисуем иконку поверх (если есть)
        if (icon != null) {
            // Рисуем иконку, например, по центру кнопки:
            int iconX = getX() + (width - 16) / 2;
            int iconY = getY() + (height - 16) / 2;
            graphics.blit(icon, iconX, iconY, 0, 0, 16, 16, 16, 16);
        }
    }
}