package com.zoritism.wirelesslinks.foundation.gui.menu;

import com.zoritism.wirelesslinks.foundation.network.ModPackets;

/**
 * Интерфейс для меню, поддерживающего очистку содержимого и отправку пакета очистки.
 */
public interface IClearableMenu {

    /**
     * Вызывается при клике на кнопку сброса, отправляет пакет на сервер.
     */
    default void sendClearPacket() {
        ModPackets.getChannel().sendToServer(new ClearMenuPacket());
    }

    /**
     * Очищает содержимое ghost-слотов или других виртуальных инвентарей.
     */
    void clearContents();
}