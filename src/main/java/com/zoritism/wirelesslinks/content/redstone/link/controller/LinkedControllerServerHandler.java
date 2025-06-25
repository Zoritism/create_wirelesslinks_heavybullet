package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.IRedstoneLinkable;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
import com.zoritism.wirelesslinks.content.redstone.link.WirelessLinkNetworkHandler;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkedControllerServerHandler {

	private static final int TIMEOUT = 30;
	private static final Logger LOGGER = LogManager.getLogger();

	private static final Map<Level, Map<UUID, Collection<ManualFrequencyEntry>>> receivedInputs = new HashMap<>();

	public static void tick(Level level) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(level);
		if (map == null)
			return;

		for (Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> it = map.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = it.next();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			for (Iterator<ManualFrequencyEntry> entryIt = list.iterator(); entryIt.hasNext(); ) {
				ManualFrequencyEntry manual = entryIt.next();
				manual.decrement();
				if (!manual.isAlive()) {
					LOGGER.info("[SERVER] Removing expired ManualFrequencyEntry: pos={}, key={}", manual.getLocation(), manual.getNetworkKey());
					WirelessLinkNetworkHandler.removeFromNetwork(level, manual);
					entryIt.remove();
				}
			}

			if (list.isEmpty())
				it.remove();
		}
	}

	/**
	 * Обрабатывает сигнал с контроллера:
	 * - Если pressed=true, поддерживает или добавляет ManualFrequencyEntry для каждой частоты
	 * - Если pressed=false, выключает соответствующую частоту для игрока
	 *
	 * Важно: если на клиенте отправляются все частоты каждый тик с pressed=true,
	 * то сигнал по всем частотам будет всегда активен.
	 */
	public static void receivePressed(Level level, BlockPos pos, UUID playerId, List<Couple<Frequency>> frequencies, boolean pressed) {
		LOGGER.info("[SERVER] receivePressed: pos={}, playerId={}, frequencies={}, pressed={}", pos, playerId, frequencies, pressed);

		Map<UUID, Collection<ManualFrequencyEntry>> worldMap =
				receivedInputs.computeIfAbsent(level, w -> new HashMap<>());
		Collection<ManualFrequencyEntry> list =
				worldMap.computeIfAbsent(playerId, id -> new ArrayList<>());

		// Обрабатываем каждую частоту отдельно
		nextKey:
		for (Couple<Frequency> key : frequencies) {
			for (ManualFrequencyEntry entry : list) {
				if (entry.getNetworkKey().equals(key)) {
					LOGGER.info("[SERVER] Existing frequency entry found for player={}, key={}, pressed={}", playerId, key, pressed);
					if (!pressed) {
						entry.setTimeout(0);
						LOGGER.info("[SERVER] Frequency disabled: setTimeout(0) for key={}", key);
					} else {
						entry.updatePosition(pos);
						LOGGER.info("[SERVER] Frequency refreshed: updatePosition({}) for key={}", pos, key);
					}
					continue nextKey;
				}
			}

			if (!pressed) {
				LOGGER.info("[SERVER] Not pressed and entry does not exist for key={}, skipping", key);
				continue;
			}

			// pressed == true и такой частоты ещё нет
			ManualFrequencyEntry newEntry = new ManualFrequencyEntry(pos, key);
			list.add(newEntry);
			WirelessLinkNetworkHandler.addToNetwork(level, newEntry);
			LOGGER.info("[SERVER] Added new ManualFrequencyEntry: pos={}, key={}, player={}", pos, key, playerId);
		}

		// Если pressed==false и какая-то частота не была найдена, ничего не делаем (она и так выключена)
	}

	public static class ManualFrequencyEntry implements IRedstoneLinkable {

		private int timeout = TIMEOUT;
		private BlockPos pos;
		private final Couple<Frequency> key;

		public ManualFrequencyEntry(BlockPos pos, Couple<Frequency> key) {
			this.pos = pos;
			this.key = key;
		}

		public void updatePosition(BlockPos pos) {
			this.pos = pos;
			this.timeout = TIMEOUT;
			LOGGER.info("[SERVER] ManualFrequencyEntry: updatePosition to {}, timeout reset to {}", pos, TIMEOUT);
		}

		public void decrement() {
			if (timeout > 0)
				timeout--;
		}

		public void setTimeout(int value) {
			this.timeout = value;
			LOGGER.info("[SERVER] ManualFrequencyEntry: setTimeout({}) for key={}", value, key);
		}

		@Override
		public boolean isAlive() {
			return timeout > 0;
		}

		@Override
		public int getTransmittedStrength() {
			return isAlive() ? 15 : 0;
		}

		@Override
		public void setReceivedStrength(int power) {}

		@Override
		public boolean isListening() {
			return false;
		}

		@Override
		public Couple<Frequency> getNetworkKey() {
			return key;
		}

		@Override
		public Couple<ItemStack> getFrequency() {
			// Возвращает пустую частоту, т.к. используется только ключ
			return Couple.of(ItemStack.EMPTY, ItemStack.EMPTY);
		}

		@Override
		public BlockPos getLocation() {
			return pos;
		}

		@Override
		public Level getLevel() {
			return null; // Не используется в текущей реализации
		}
	}
}