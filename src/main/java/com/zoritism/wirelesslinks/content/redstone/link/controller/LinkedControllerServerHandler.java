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
 * - Сигналы держатся TIMEOUT тиков после последнего нажатия/обновления.
 * - Каждая кнопка/канал = ManualFrequencyEntry, обновляется при нажатии, сбрасывается по таймеру.
 * - tick() вызывается каждый серверный тик, чтобы очищать "залипшие" сигналы.
 */
public class LinkedControllerServerHandler {

	public static final int TIMEOUT = 30;

	/** Map: UUID игрока -> список ManualFrequencyEntry (активные каналы контроллера) */
	private static final Map<UUID, Collection<ManualFrequencyEntry>> receivedInputs = new HashMap<>();

	/**
	 * Обновление состояний сигналов контроллеров.
	 * Вызывать в серверном тике (ServerTickEvent, либо world.tick()).
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
	 * - pressed=true: обновляет/создаёт ManualFrequencyEntry (продлевает таймер)
	 * - pressed=false: вручную сбрасывает таймер в 0 (моментальный сброс)
	 */
	public static void receivePressed(Level level, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (level == null || playerId == null || frequencies == null)
			return;

		Collection<ManualFrequencyEntry> list = receivedInputs.computeIfAbsent(playerId, $ -> new ArrayList<>());

		WithNext:
		for (Couple<ItemStack> activated : frequencies) {
			for (ManualFrequencyEntry entry : list) {
				if (entry.getFrequency().equals(activated)) {
					if (!pressed) {
						entry.setTimeout(0);
						LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
					} else {
						entry.updatePosition(pos);
						entry.setTimeout(TIMEOUT);
						LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
					}
					continue WithNext;
				}
			}

			if (!pressed)
				continue;

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