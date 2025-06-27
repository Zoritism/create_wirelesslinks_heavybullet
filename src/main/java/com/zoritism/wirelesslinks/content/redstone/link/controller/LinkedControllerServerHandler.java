package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.LinkHandler;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Серверная логика Linked Controller теперь полностью повторяет механику Create:
 * - Сигналы держатся только пока кнопка удерживается (pressed=true), сбрасываются мгновенно при отпускании (pressed=false).
 * - Каждая кнопка/канал = ManualFrequencyEntry, обновляется при нажатии, удаляется при отпускании.
 * - tick() вызывается только для очистки залипших сигналов при обрыве соединения (если release-пакет не пришёл).
 */
public class LinkedControllerServerHandler {

	public static final int TIMEOUT = 30;

	/** Map: UUID игрока -> список ManualFrequencyEntry (активные каналы контроллера) */
	private static final Map<UUID, Collection<ManualFrequencyEntry>> receivedInputs = new HashMap<>();

	/**
	 * Обновление состояний сигналов контроллеров.
	 * Вызывать в серверном тике (ServerTickEvent, либо world.tick()).
	 * Очищает "залипшие" сигналы (например, если release-пакет не пришёл вообще).
	 */
	public static void tick(Level level) {
		Iterator<Entry<UUID, Collection<ManualFrequencyEntry>>> mainIt = receivedInputs.entrySet().iterator();
		while (mainIt.hasNext()) {
			Entry<UUID, Collection<ManualFrequencyEntry>> entry = mainIt.next();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			Iterator<ManualFrequencyEntry> subIt = list.iterator();
			while (subIt.hasNext()) {
				ManualFrequencyEntry freqEntry = subIt.next();
				freqEntry.decrement();
				if (!freqEntry.isAlive()) {
					LinkHandler.get(level).removeVirtualTransmitter(freqEntry.getFrequency(), entry.getKey());
					subIt.remove();
				}
			}

			if (list.isEmpty())
				mainIt.remove();
		}
	}

	/**
	 * Приходит при нажатии или отпускании кнопки на контроллере.
	 * - pressed=true: создаёт ManualFrequencyEntry (если нет), включает сигнал и устанавливает TIMEOUT.
	 * - pressed=false: немедленно удаляет запись (и выключает сигнал).
	 */
	public static void receivePressed(Level level, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (level == null || playerId == null || frequencies == null)
			return;

		Collection<ManualFrequencyEntry> list = receivedInputs.computeIfAbsent(playerId, $ -> new ArrayList<>());

		WithNext:
		for (Couple<ItemStack> activated : frequencies) {
			Iterator<ManualFrequencyEntry> it = list.iterator();
			while (it.hasNext()) {
				ManualFrequencyEntry entry = it.next();
				if (entry.getFrequency().equals(activated)) {
					if (!pressed) {
						// Сброс сигнала: удаляем entry и снимаем виртуальный передатчик
						LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
						it.remove();
					} else {
						// Обновляем позицию и таймер (если вдруг пришло повторное нажатие)
						entry.updatePosition(pos);
						entry.setTimeout(TIMEOUT);
						LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
					}
					continue WithNext;
				}
			}

			if (!pressed)
				continue;

			// Если нет такой записи, создаём новую и включаем сигнал
			ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, activated, TIMEOUT);
			list.add(entry);
			LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
		}
	}

	/**
	 * Удалить все сигналы игрока (вызывать при дисконнекте/смерти/выбросе контроллера и т.д.)
	 */
	public static void removeAllInputs(Level level, UUID playerId) {
		Collection<ManualFrequencyEntry> list = receivedInputs.remove(playerId);
		if (list != null) {
			for (ManualFrequencyEntry entry : list) {
				LinkHandler.get(level).removeVirtualTransmitter(entry.getFrequency(), playerId);
			}
		}
	}

	/**
	 * ManualFrequencyEntry — активный канал контроллера.
	 * Аналогичен ManualFrequencyEntry в Create.
	 */
	public static class ManualFrequencyEntry {
		private int timeout;
		private final Couple<ItemStack> frequency;
		private BlockPos pos;

		public ManualFrequencyEntry(BlockPos pos, Couple<ItemStack> frequency, int timeout) {
			this.pos = pos;
			this.frequency = frequency;
			this.timeout = timeout;
		}

		public void updatePosition(BlockPos pos) {
			this.pos = pos;
		}

		public void decrement() {
			if (timeout > 0)
				timeout--;
		}

		public boolean isAlive() {
			return timeout > 0;
		}

		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}

		public Couple<ItemStack> getFrequency() {
			return frequency;
		}

		public BlockPos getPosition() {
			return pos;
		}
	}
}