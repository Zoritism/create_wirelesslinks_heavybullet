package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IRedstoneLinkable {

    /**
     * Позиция, в которой находится этот элемент (для идентификации в сети).
     */
    BlockPos getLocation();

    /**
     * Уровень (мир), в котором работает элемент.
     */
    Level getLevel();

    /**
     * Сила сигнала, которую этот элемент передаёт в сеть (только для передатчиков).
     */
    int getTransmittedStrength();

    /**
     * Установить силу сигнала, полученного из сети (только для приёмников).
     */
    void setReceivedStrength(int power);

    /**
     * Частотная пара, определяющая уникальный ключ подключения.
     */
    Couple<Frequency> getNetworkKey();

    /**
     * Возвращает true, если этот элемент слушает сигналы (приёмник).
     */
    boolean isListening();

    /**
     * Возвращает true, если элемент ещё активен и не должен быть удалён из сети.
     */
    default boolean isAlive() {
        return true;
    }

    /**
     * Возвращает текущую частотную пару (представление как ItemStack'и).
     * Используется для вычисления канала.
     */
    Couple<ItemStack> getFrequency();
}
