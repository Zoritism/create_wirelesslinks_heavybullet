package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.IRedstoneLinkable;
import com.zoritism.wirelesslinks.content.redstone.link.LinkHandler;
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
				LOGGER.info("[LinkedControllerServerHandler][tick] Decremented entry: pos={}, key={}, timeout={}", manual.getLocation(), manual.getNetworkKey(), manual.timeout);
				if (!manual.isAlive()) {
					LOGGER.info("[LinkedControllerServerHandler][tick] Removing expired ManualFrequencyEntry: pos={}, key={}", manual.getLocation(), manual.getNetworkKey());
					LinkHandler.get(level).removeLink(manual);
					WirelessLinkNetworkHandler.removeFromNetwork(level, manual);
					entryIt.remove();
				}
			}

			if (list.isEmpty()) {
				LOGGER.info("[LinkedControllerServerHandler][tick] Removing empty entry list for player={}", entry.getKey());
				it.remove();
			}
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
		LOGGER.info("[LinkedControllerServerHandler][receivePressed] pos={}, playerId={}, frequencies={}, pressed={}", pos, playerId, frequencies, pressed);

		Map<UUID, Collection<ManualFrequencyEntry>> worldMap =
				receivedInputs.computeIfAbsent(level, w -> new HashMap<>());
		Collection<ManualFrequencyEntry> list =
				worldMap.computeIfAbsent(playerId, id -> new ArrayList<>());

		// Обрабатываем каждую частоту отдельно
		nextKey:
		for (Couple<Frequency> key : frequencies) {
			LOGGER.info("[LinkedControllerServerHandler][receivePressed] Processing key: {}", key);
			for (ManualFrequencyEntry entry : list) {
				LOGGER.info("[LinkedControllerServerHandler][receivePressed] Comparing with existing entry: key={}, pressed={}, entryKey={}", key, pressed, entry.getNetworkKey());
				if (entry.getNetworkKey().equals(key)) {
					LOGGER.info("[LinkedControllerServerHandler][receivePressed] Existing frequency entry found for player={}, key={}, pressed={}", playerId, key, pressed);
					if (!pressed) {
						entry.setTimeout(0);
						LOGGER.info("[LinkedControllerServerHandler][receivePressed] Frequency disabled: setTimeout(0) for key={}", key);
						LinkHandler.get(level).removeLink(entry);
						WirelessLinkNetworkHandler.removeFromNetwork(level, entry);
					} else {
						entry.updatePosition(pos);
						LOGGER.info("[LinkedControllerServerHandler][receivePressed] Frequency refreshed: updatePosition({}) for key={}", pos, key);
						LinkHandler.get(level).updateLink(entry);
						WirelessLinkNetworkHandler.addToNetwork(level, entry);
					}
					continue nextKey;
				}
			}

			if (!pressed) {
				LOGGER.info("[LinkedControllerServerHandler][receivePressed] Not pressed and entry does not exist for key={}, skipping", key);
				continue;
			}

			// pressed == true и такой частоты ещё нет
			ManualFrequencyEntry newEntry = new ManualFrequencyEntry(pos, key);
			list.add(newEntry);
			LinkHandler.get(level).updateLink(newEntry);
			WirelessLinkNetworkHandler.addToNetwork(level, newEntry);
			LOGGER.info("[LinkedControllerServerHandler][receivePressed] Added new ManualFrequencyEntry: pos={}, key={}, player={}", pos, key, playerId);
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
			LOGGER.info("[ManualFrequencyEntry][create] pos={}, key={}, timeout={}", pos, key, timeout);
		}

		public void updatePosition(BlockPos pos) {
			this.pos = pos;
			this.timeout = TIMEOUT;
			LOGGER.info("[ManualFrequencyEntry][updatePosition] pos={}, key={}, timeout reset to {}", pos, key, TIMEOUT);
		}

		public void decrement() {
			if (timeout > 0)
				timeout--;
			LOGGER.info("[ManualFrequencyEntry][decrement] pos={}, key={}, timeout={}", pos, key, timeout);
		}

		public void setTimeout(int value) {
			this.timeout = value;
			LOGGER.info("[ManualFrequencyEntry][setTimeout] pos={}, key={}, timeout={}", pos, key, value);
		}

		@Override
		public boolean isAlive() {
			boolean alive = timeout > 0;
			LOGGER.info("[ManualFrequencyEntry][isAlive] pos={}, key={}, timeout={}, alive={}", pos, key, timeout, alive);
			return alive;
		}

		@Override
		public int getTransmittedStrength() {
			int strength = isAlive() ? 15 : 0;
			LOGGER.info("[ManualFrequencyEntry][getTransmittedStrength] pos={}, key={}, strength={}", pos, key, strength);
			return strength;
		}

		@Override
		public void setReceivedStrength(int power) {
			LOGGER.info("[ManualFrequencyEntry][setReceivedStrength] pos={}, key={}, receivedPower={}", pos, key, power);
		}

		@Override
		public boolean isListening() {
			LOGGER.info("[ManualFrequencyEntry][isListening] pos={}, key={}, returns false (always transmitter)", pos, key);
			return false;
		}

		@Override
		public Couple<Frequency> getNetworkKey() {
			LOGGER.info("[ManualFrequencyEntry][getNetworkKey] pos={}, key={}", pos, key);
			return key;
		}

		@Override
		public Couple<ItemStack> getFrequency() {
			// Нормализуем ItemStack как в RedstoneLinkBlockEntity
			ItemStack a = key.getFirst() == null ? ItemStack.EMPTY : normalize(key.getFirst().getStack());
			ItemStack b = key.getSecond() == null ? ItemStack.EMPTY : normalize(key.getSecond().getStack());
			LOGGER.info("[ManualFrequencyEntry][getFrequency] pos={}, key={}, freqA={}, freqB={}", pos, key, a, b);
			return Couple.of(a, b);
		}

		private static ItemStack normalize(ItemStack in) {
			if (in == null || in.isEmpty())
				return ItemStack.EMPTY;
			ItemStack copy = in.copy();
			copy.setTag(null);
			return copy;
		}

		@Override
		public BlockPos getLocation() {
			LOGGER.info("[ManualFrequencyEntry][getLocation] pos={}, key={}", pos, key);
			return pos;
		}

		@Override
		public Level getLevel() {
			LOGGER.info("[ManualFrequencyEntry][getLevel] pos={}, key={}", pos, key);
			return null; // Не используется в текущей реализации
		}
	}
}