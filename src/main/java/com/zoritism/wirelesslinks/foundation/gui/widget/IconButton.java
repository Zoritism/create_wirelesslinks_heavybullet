package com.zoritism.wirelesslinks.foundation.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Кнопка с иконкой, максимально повторяющая логику Create, но не зависящая от Create.
 * Поддерживает кастомизацию цвета (green), смену иконки, тултип, обработку клика.
 */
public class IconButton extends AbstractWidget {
    protected ResourceLocation icon;
    protected boolean green = false;
    protected boolean active = true;
    protected boolean visible = true;
    protected List<Component> toolTip = new ArrayList<>();
    protected Consumer<IconButton> onPress = btn -> {};

    public IconButton(int x, int y, ResourceLocation icon) {
        this(x, y, 18, 18, icon);
    }

    public IconButton(int x, int y, int width, int height, ResourceLocation icon) {
        super(x, y, width, height, Component.empty());
        this.icon = icon;
    }

    /**
     * Установить обработчик клика.
     */
    public IconButton withCallback(Runnable callback) {
        this.onPress = btn -> callback.run();
        return this;
    }

    /**
     * Установить обработчик клика с доступом к кнопке.
     */
    public IconButton withCallback(Consumer<IconButton> callback) {
        this.onPress = callback;
        return this;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (isActive() && isVisible()) {
            onPress.accept(this);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        // Фон кнопки: простой rect или кастомный ресурс
        int color = !active ? 0xFFAAAAAA : isHovered() ? 0xFFE0E0E0 : (green ? 0xFF70D080 : 0xFFFFFFFF);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);

        // Иконка по центру
        if (icon != null) {
            int iconX = getX() + (width - 16) / 2;
            int iconY = getY() + (height - 16) / 2;
            graphics.blit(icon, iconX, iconY, 0, 0, 16, 16, 16, 16);
        }
    }

    public void setToolTip(Component text) {
        toolTip.clear();
        toolTip.add(text);
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }

    public void setGreen(boolean green) {
        this.green = green;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<Component> getToolTip() {
        return toolTip;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narration) {
        if (!toolTip.isEmpty()) {
            narration.add(NarratedElementType.TITLE, toolTip.get(0));
        } else {
            narration.add(NarratedElementType.TITLE, this.getMessage());
        }
    }
}