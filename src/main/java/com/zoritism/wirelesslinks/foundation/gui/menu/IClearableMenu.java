package com.zoritism.wirelesslinks.foundation.gui.menu;

/**
 * Интерфейс для меню, поддерживающего очистку содержимого и отправку пакета очистки.
 */
public interface IClearableMenu {

    /**
     * Вызывается при клике на кнопку сброса, отправляет пакет на сервер (реализуй в своём меню при необходимости).
     */
    default void sendClearPacket() {
        // Для Forge/Fabric реализовать отправку пакета, если нужно.
        // Например:
        // ModPackets.INSTANCE.sendToServer(new ClearMenuPacket());
    }

    /**
     * Очищает содержимое ghost-слотов или других виртуальных инвентарей.
     */
    void clearContents();
}