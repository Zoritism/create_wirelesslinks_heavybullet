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
 * - Таймаут уменьшается только если ни одна из кнопок не удерживается — если хотя бы одна всё ещё удерживается, таймаут сбрасывается.
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
	 * Для каждого игрока и для каждого канала (частоты) — сколько раз держится (нажатий), чтобы поддерживать сигнал пока хотя бы одна кнопка не отпущена
	 */
	private static final WorldAttached<Map<UUID, Map<Couple<ItemStack>, Integer>>> heldCounts =
			new WorldAttached<>($ -> new HashMap<>());

	public static void tick(LevelAccessor world) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Map<UUID, Map<Couple<ItemStack>, Integer>> heldMap = heldCounts.get(world);

		Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> mainIt = map.entrySet().iterator();
		while (mainIt.hasNext()) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = mainIt.next();
			UUID playerId = entry.getKey();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			Map<Couple<ItemStack>, Integer> heldCountsForPlayer = heldMap.getOrDefault(playerId, Collections.emptyMap());

			Iterator<ManualFrequencyEntry> subIt = list.iterator();
			while (subIt.hasNext()) {
				ManualFrequencyEntry freqEntry = subIt.next();

				// Если канал всё ещё удерживается (heldCount > 0), сбрасываем таймаут
				int count = heldCountsForPlayer.getOrDefault(freqEntry.getFrequency(), 0);
				if (count > 0) {
					freqEntry.setTimeout(TIMEOUT);
				} else {
					freqEntry.decrement();
				}
				// heldThisTick не нужен больше

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
	}

	/**
	 * Приходит при нажатии или отпускании кнопки на контроллере.
	 * - pressed=true: если нет ManualFrequencyEntry — создаёт, если есть — увеличивает heldCount для этой частоты.
	 * - pressed=false: уменьшает heldCount (и при 0 — удаляет запись и выключает сигнал).
	 */
	public static void receivePressed(LevelAccessor world, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (world == null || playerId == null || frequencies == null)
			return;

		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Collection<ManualFrequencyEntry> list = map.computeIfAbsent(playerId, $ -> new ArrayList<>());

		Map<UUID, Map<Couple<ItemStack>, Integer>> heldMap = heldCounts.get(world);
		Map<Couple<ItemStack>, Integer> heldCountForPlayer = heldMap.computeIfAbsent(playerId, $ -> new HashMap<>());

		for (Couple<ItemStack> activated : frequencies) {
			ManualFrequencyEntry matched = null;
			for (ManualFrequencyEntry entry : list) {
				if (entry.getFrequency().equals(activated)) {
					matched = entry;
					break;
				}
			}
			if (pressed) {
				if (matched != null) {
					matched.updatePosition(pos);
				} else {
					ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, activated, TIMEOUT);
					list.add(entry);
				}

				// Увеличиваем heldCount для частоты
				heldCountForPlayer.put(activated, heldCountForPlayer.getOrDefault(activated, 0) + 1);

				// В любом случае поддерживаем виртуальный передатчик активным
				if (world instanceof Level level)
					LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
			} else {
				// Уменьшаем heldCount для частоты
				int c = heldCountForPlayer.getOrDefault(activated, 0);
				if (c > 1) {
					heldCountForPlayer.put(activated, c - 1);
				} else {
					heldCountForPlayer.remove(activated); // больше не держится

					if (matched != null) {
						if (world instanceof Level level) {
							LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
							LinkHandler.get(level).refreshChannel(activated);
						}
						list.remove(matched);
					} else if (world instanceof Level level) {
						LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
						LinkHandler.get(level).refreshChannel(activated);
					}
				}
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
		Map<UUID, Map<Couple<ItemStack>, Integer>> heldMap = heldCounts.get(world);
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