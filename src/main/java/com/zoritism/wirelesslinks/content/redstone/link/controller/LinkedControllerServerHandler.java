package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.IRedstoneLinkable;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
import com.zoritism.wirelesslinks.content.redstone.link.WirelessLinkNetworkHandler;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public class LinkedControllerServerHandler {

	private static final int TIMEOUT = 30;

	private static final Map<Level, Map<UUID, Collection<ManualFrequencyEntry>>> receivedInputs = new HashMap<>();

	public static void tick(Level world) {
		Map<UUID, Collection<ManualFrequencyEntry>> map = receivedInputs.get(world);
		if (map == null)
			return;

		for (Iterator<Map.Entry<UUID, Collection<ManualFrequencyEntry>>> it = map.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Collection<ManualFrequencyEntry>> entry = it.next();
			Collection<ManualFrequencyEntry> list = entry.getValue();

			for (Iterator<ManualFrequencyEntry> entryIt = list.iterator(); entryIt.hasNext(); ) {
				ManualFrequencyEntry manual = entryIt.next();
				manual.decrement();
				if (!manual.isAlive()) {
					WirelessLinkNetworkHandler.removeFromNetwork(world, manual);
					entryIt.remove();
				}
			}

			if (list.isEmpty())
				it.remove();
		}
	}

	public static void receivePressed(Level world, BlockPos pos, UUID playerId, List<Couple<Frequency>> keys, boolean pressed) {
		Map<UUID, Collection<ManualFrequencyEntry>> worldMap =
				receivedInputs.computeIfAbsent(world, w -> new HashMap<>());
		Collection<ManualFrequencyEntry> list =
				worldMap.computeIfAbsent(playerId, id -> new ArrayList<>());

		nextKey:
		for (Couple<Frequency> key : keys) {
			for (ManualFrequencyEntry entry : list) {
				if (entry.getNetworkKey().equals(key)) {
					if (!pressed)
						entry.setTimeout(0);
					else
						entry.updatePosition(pos);
					continue nextKey;
				}
			}

			if (!pressed)
				continue;

			ManualFrequencyEntry newEntry = new ManualFrequencyEntry(pos, key);
			list.add(newEntry);
			WirelessLinkNetworkHandler.addToNetwork(world, newEntry);
		}
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
		}

		public void decrement() {
			if (timeout > 0)
				timeout--;
		}

		public void setTimeout(int value) {
			this.timeout = value;
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
			// Возвращает пустую частоту, так как используется только ключ
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
