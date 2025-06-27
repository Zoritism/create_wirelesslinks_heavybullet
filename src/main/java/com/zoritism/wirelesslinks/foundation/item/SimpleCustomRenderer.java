package com.zoritism.wirelesslinks.foundation.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Аналог SimpleCustomRenderer из Create.
 * Регистрирует кастомный рендерер для предмета и возвращает его через IClientItemExtensions.
 */
public class SimpleCustomRenderer implements IClientItemExtensions {

    protected final CustomRenderedItemModelRenderer renderer;

    protected SimpleCustomRenderer(CustomRenderedItemModelRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Создаёт и возвращает расширение для клиента, регистрируя предмет как кастомно рендеримый.
     * @param item предмет, для которого применяется рендерер
     * @param renderer кастомный рендерер (ваш наследник CustomRenderedItemModelRenderer)
     * @return расширение для клиента
     */
    public static SimpleCustomRenderer create(Item item, CustomRenderedItemModelRenderer renderer) {
        CustomRenderedItems.register(item);
        return new SimpleCustomRenderer(renderer);
    }

    @Override
    public CustomRenderedItemModelRenderer getCustomRenderer() {
        return renderer;
    }
}