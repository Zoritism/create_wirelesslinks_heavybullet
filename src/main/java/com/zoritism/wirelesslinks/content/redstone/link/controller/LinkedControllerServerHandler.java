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
 * - Сигналы держатся пока кнопка удерживается (pressed=true) или до истечения таймаута (если release не пришёл).
 * - Таймаут ПРОДЛЕВАЕТСЯ только когда реально приходит новый pressed=true от игрока (каждый тик от клиента!).
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

	public static void tick(LevelAccessor world) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> mainIt = map.entrySet().iterator();
		while (mainIt.hasNext()) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = mainIt.next();
			UUID playerId = entry.getKey();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			Iterator<ManualFrequencyEntry> subIt = list.iterator();
			while (subIt.hasNext()) {
				ManualFrequencyEntry freqEntry = subIt.next();
				// Таймер декрементируется ТОЛЬКО если не было продления в этом тике!
				if (!freqEntry.touchedThisTick) {
					freqEntry.decrement();
				}
				freqEntry.touchedThisTick = false; // сброс на следующий тик

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
	 * - pressed=true: если нет ManualFrequencyEntry — создаёт, если есть — продлевает timeout (и помечает как активный в этом тике).
	 * - pressed=false: немедленно удаляет запись (и выключает сигнал).
	 */
	public static void receivePressed(LevelAccessor world, BlockPos pos, UUID playerId, List<Couple<ItemStack>> frequencies, boolean pressed) {
		if (world == null || playerId == null || frequencies == null)
			return;

		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		Collection<ManualFrequencyEntry> list = map.computeIfAbsent(playerId, $ -> new ArrayList<>());

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
					// Продлеваем timeout и отмечаем, что этот entry был активен в этом тике
					matched.updatePosition(pos);
					matched.setTimeout(TIMEOUT);
					matched.touchedThisTick = true;
				} else {
					// Если нет такой записи, создаём новую и включаем сигнал
					ManualFrequencyEntry entry = new ManualFrequencyEntry(pos, activated, TIMEOUT);
					entry.touchedThisTick = true;
					list.add(entry);
				}
				// В любом случае поддерживаем виртуальный передатчик активным
				if (world instanceof Level level)
					LinkHandler.get(level).setVirtualTransmitter(activated, playerId, 15);
			} else {
				boolean found = false;
				if (matched != null) {
					if (world instanceof Level level) {
						LinkHandler.get(level).removeVirtualTransmitter(activated, playerId);
						LinkHandler.get(level).refreshChannel(activated);
					}
					list.remove(matched);
					found = true;
				}
				if (!found && world instanceof Level level) {
					// Если не нашли ManualFrequencyEntry (например, истёк timeout), всё равно обновляем канал!
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
	}

	/**
	 * ManualFrequencyEntry — активный канал контроллера.
	 */
	public static class ManualFrequencyEntry {
		private int timeout;
		private final Couple<ItemStack> frequency;
		private BlockPos pos;

		// Был ли продлён таймер в этом тике (то есть пришёл pressed=true)
		private boolean touchedThisTick = false;

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