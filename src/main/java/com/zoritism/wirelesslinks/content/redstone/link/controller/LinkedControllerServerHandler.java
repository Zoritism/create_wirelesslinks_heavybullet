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
 * Серверная логика Linked Controller:
 * - Сигнал держится, пока сервер хотя бы раз за тик получает pressed=true для частоты.
 * - Если ни одного pressed=true не пришло за тик — канал декрементирует таймаут; если истёк — выключается.
 * - pressed=false сбрасывает сигнал немедленно.
 * - После обработки тика состояние удержания очищается (каждый тик клиент должен подтверждать, что кнопка всё ещё зажата!).
 */
public class LinkedControllerServerHandler {

	public static final int TIMEOUT = 30;

	/**
	 * WorldAttached: LevelAccessor -> Map<UUID, Collection<ManualFrequencyEntry>>
	 */
	private static final WorldAttached<Map<UUID, Collection<ManualFrequencyEntry>>> receivedInputs =
			new WorldAttached<>($ -> new HashMap<>());

	/**
	 * Для каждого игрока: какие частоты были удержаны хотя бы раз за тик (pressed=true).
	 * После tick() очищается.
	 */
	private static final WorldAttached<Map<UUID, Set<Couple<ItemStack>>>> heldThisTick =
			new WorldAttached<>($ -> new HashMap<>());

	public static void tick(LevelAccessor world) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Map<UUID, Set<Couple<ItemStack>>> heldMap = heldThisTick.get(world);

		Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> mainIt = map.entrySet().iterator();
		while (mainIt.hasNext()) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = mainIt.next();
			UUID playerId = entry.getKey();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			Set<Couple<ItemStack>> heldFrequencies = heldMap.getOrDefault(playerId, Collections.emptySet());

			Iterator<ManualFrequencyEntry> subIt = list.iterator();
			while (subIt.hasNext()) {
				ManualFrequencyEntry freqEntry = subIt.next();

				// Если сигнал был удержан хотя бы раз за этот тик — сбрасываем таймаут
				if (heldFrequencies.contains(freqEntry.getFrequency())) {
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
		// После тика очищаем heldThisTick — клиент должен подтверждать удержание каждую итерацию.
		heldMap.clear();
	}

	/**
	 * Приходит при нажатии или удержании кнопки на контроллере.
	 * - pressed=true: если нет ManualFrequencyEntry — создаёт, если есть — помечает как "удерживается в этом тике".
	 * - pressed=false: немедленно удаляет запись (и выключает сигнал).
	 */
	public static void receivePressed(LevelAccessor world, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (world == null || playerId == null || frequencies == null)
			return;

		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Collection<ManualFrequencyEntry> list = map.computeIfAbsent(playerId, $ -> new ArrayList<>());

		Map<UUID, Set<Couple<ItemStack>>> heldMap = heldThisTick.get(world);
		Set<Couple<ItemStack>> heldSet = heldMap.computeIfAbsent(playerId, $ -> new HashSet<>());

		for (Couple<ItemStack> activated : frequencies) {
			ManualFrequencyEntry matched = null;
			for (ManualFrequencyEntry entry : list) {
				if (entry.getFrequency().equals(activated)) {
					matched = entry;
					break;
				}
			}
			if (pressed) {
				heldSet.add(activated);
				if (matched != null) {
					matched.updatePosition(pos);
				} else {
					ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, activated, TIMEOUT);
					list.add(entry);
				}
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
					LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
					LinkHandler.get(level).refreshChannel(activated);
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
		Map<UUID, Set<Couple<ItemStack>>> heldMap = heldThisTick.get(world);
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