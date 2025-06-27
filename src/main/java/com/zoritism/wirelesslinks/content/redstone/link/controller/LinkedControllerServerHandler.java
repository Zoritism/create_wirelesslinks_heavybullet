package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.LinkHandler;
import com.zoritism.wirelesslinks.util.Couple;
import com.zoritism.wirelesslinks.util.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;

/**
 * Серверная логика Linked Controller максимально по Create, но с нашими частотами Couple<ItemStack>.
 * - Сигналы держатся пока кнопка удерживается (pressed=true), сбрасываются только по отпусканию или если pressed=true перестал приходить (таймаут).
 * - Таймаут уменьшается только если pressed=true не пришло ни разу за тик; если пришло - таймаут сбрасывается.
 * - Только release или истечение таймаута сбрасывает сигнал и виртуальный передатчик.
 * - Используется WorldAttached для правильной работы в мульти-мире.
 */
public class LinkedControllerServerHandler {

	// Таймаут в тиках (1.5 секунды при 20 TPS)
	public static final int TIMEOUT = 30;

	/**
	 * WorldAttached: LevelAccessor -> Map<UUID, Collection<ManualFrequencyEntry>>
	 */
	private static final WorldAttached<Map<UUID, Collection<ManualFrequencyEntry>>> receivedInputs =
			new WorldAttached<>($ -> new HashMap<>());

	/**
	 * Для отслеживания "держится ли кнопка в этом тике" для каждой частоты (playerId -> freq -> heldThisTick)
	 */
	private static final WorldAttached<Map<UUID, Map<Couple<ItemStack>, Boolean>>> heldThisTick =
			new WorldAttached<>($ -> new HashMap<>());

	public static void tick(LevelAccessor world) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Map<UUID, Map<Couple<ItemStack>, Boolean>> heldMap = heldThisTick.get(world);

		Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> mainIt = map.entrySet().iterator();
		while (mainIt.hasNext()) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = mainIt.next();
			UUID playerId = entry.getKey();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			Map<Couple<ItemStack>, Boolean> heldForPlayer = heldMap.getOrDefault(playerId, Collections.emptyMap());

			Iterator<ManualFrequencyEntry> subIt = list.iterator();
			while (subIt.hasNext()) {
				ManualFrequencyEntry freqEntry = subIt.next();

				// Если канал был активен в этом тике (pressed=true), сбрасываем таймаут
				boolean held = heldForPlayer.getOrDefault(freqEntry.getFrequency(), false);
				if (held) {
					freqEntry.setTimeout(TIMEOUT);
				} else {
					freqEntry.decrement();
				}

				if (!freqEntry.isAlive()) {
					if (world instanceof Level level) {
						LinkHandler.get(level).removeVirtualTransmitter(freqEntry.getFrequency(), playerId);
						LinkHandler.get(level).refreshChannel(freqEntry.getFrequency());
					}
					subIt.remove();
				}
			}
			if (list.isEmpty())
				mainIt.remove();
		}
		// Очищаем heldThisTick на следующий тик (иначе "зависшие" каналы не выключатся)
		heldMap.clear();
	}

	/**
	 * Приходит при нажатии или отпускании кнопки на контроллере.
	 * - pressed=true: если нет ManualFrequencyEntry — создаёт, если есть — помечает как активный в этом тике.
	 * - pressed=false: немедленно удаляет запись (и выключает сигнал).
	 */
	public static void receivePressed(LevelAccessor world, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (world == null || playerId == null || frequencies == null)
			return;

		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Collection<ManualFrequencyEntry> list = map.computeIfAbsent(playerId, $ -> new ArrayList<>());

		Map<UUID, Map<Couple<ItemStack>, Boolean>> heldMap = heldThisTick.get(world);
		Map<Couple<ItemStack>, Boolean> heldForPlayer = heldMap.computeIfAbsent(playerId, $ -> new HashMap<>());

		for (Couple<ItemStack> activated : frequencies) {
			ManualFrequencyEntry matched = null;
			for (ManualFrequencyEntry entry : list) {
				if (entry.getFrequency().equals(activated)) {
					matched = entry;
					break;
				}
			}
			if (pressed) {
				heldForPlayer.put(activated, true);
				if (matched != null) {
					// Обновляем позицию
					matched.updatePosition(pos);
				} else {
					// Если нет такой записи, создаём новую и включаем сигнал
					ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, activated, TIMEOUT);
					list.add(entry);
				}
				// В любом случае поддерживаем виртуальный передатчик активным
				if (world instanceof Level level)
					LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
			} else {
				if (matched != null) {
					if (world instanceof Level level) {
						LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
						LinkHandler.get(level).refreshChannel(activated);
					}
					list.remove(matched);
				} else if (world instanceof Level level) {
					// Если не нашли ManualFrequencyEntry (например, истёк timeout), всё равно обновляем канал!
					LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
					LinkHandler.get(level).refreshChannel(activated);
				}
				// Удаляем состояние held для этого канала (на случай release)
				heldForPlayer.remove(activated);
			}
		}
	}

	/**
	 * Удалить все сигналы игрока (вызывать при дисконнекте/смерти).
	 */
	public static void removeAllInputs(LevelAccessor world, UUID playerId) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Collection<ManualFrequencyEntry> list = map.remove(playerId);
		if (list != null && world instanceof Level level) {
			for (ManualFrequencyEntry entry : list) {
				LinkHandler.get(level).removeVirtualTransmitter(entry.getFrequency(), playerId);
				LinkHandler.get(level).refreshChannel(entry.getFrequency());
			}
		}
		Map<UUID, Map<Couple<ItemStack>, Boolean>> heldMap = heldThisTick.get(world);
		heldMap.remove(playerId);
	}

	/**
	 * ManualFrequencyEntry — активный канал контроллера.
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