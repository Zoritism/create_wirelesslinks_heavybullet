package com.zoritism.wirelesslinks.foundation.gui.widget;

import com.zoritism.wirelesslinks.foundation.gui.AllIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Кнопка с иконкой как в Create — принимает AllIcons.
 */
public class IconButton extends AbstractWidget {
    protected AllIcons icon;
    protected boolean green = false;
    protected boolean active = true;
    protected boolean visible = true;
    protected List<Component> toolTip = new ArrayList<>();
    protected Consumer<IconButton> onPress = btn -> {};

    public IconButton(int x, int y, AllIcons icon) {
        this(x, y, AllIcons.ICON_SIZE + 2, AllIcons.ICON_SIZE + 2, icon);
    }

    public IconButton(int x, int y, int width, int height, AllIcons icon) {
        super(x, y, width, height, Component.empty());
        this.icon = icon;
    }

    public IconButton withCallback(Runnable callback) {
        this.onPress = btn -> callback.run();
        return this;
    }

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
        int color = !active ? 0xFFAAAAAA : isHovered() ? 0xFFE0E0E0 : (green ? 0xFF70D080 : 0xFFFFFFFF);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);

        if (icon != null) {
            int iconX = getX() + (width - AllIcons.ICON_SIZE) / 2;
            int iconY = getY() + (height - AllIcons.ICON_SIZE) / 2;
            icon.render(graphics, iconX, iconY);
        }
    }

    public void setToolTip(Component text) {
        toolTip.clear();
        toolTip.add(text);
    }

    public void setIcon(AllIcons icon) {
        this.icon = icon;
    }

    public void setGreen(boolean green) {
        this.green = green;
    }

    @Override
    public boolean isActive() {
        return active;
    }

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

    public void updateWidgetNarration(NarrationElementOutput narration) {
        if (!toolTip.isEmpty()) {
            narration.add(NarratedElementType.TITLE, toolTip.get(0));
        } else {
            narration.add(NarratedElementType.TITLE, this.getMessage());
        }
    }
}