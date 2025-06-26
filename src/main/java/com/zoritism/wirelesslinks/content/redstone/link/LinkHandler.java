package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Менеджер сетей Redstone Link с поддержкой частот.
 */
public class LinkHandler extends SavedData {
	private static final String NAME = "wirelesslinks_data";
	private static LinkHandler clientInstance;

	private final Map<Couple<ItemStack>, Set<IRedstoneLinkable>> transmittersByFrequency = new HashMap<>();
	private final Map<Couple<ItemStack>, Set<IRedstoneLinkable>> receiversByFrequency = new HashMap<>();

	public static LinkHandler get(Level level) {
		if (level.isClientSide)
			return clientInstance == null ? (clientInstance = new LinkHandler()) : clientInstance;

		ServerLevel server = (ServerLevel) level;
		return server.getDataStorage().computeIfAbsent(
				nbt -> {
					LinkHandler h = new LinkHandler();
					h.load(nbt);
					return h;
				}, LinkHandler::new, NAME);
	}

	// === ДОбАВЛЕН НОВЫЙ МЕТОД ===
	public void addLink(IRedstoneLinkable link) {
		updateLink(link);
	}

	public void updateLink(IRedstoneLinkable link) {
		if (link == null)
			return;

		Couple<ItemStack> oldFrequency = link.getFrequency();
		removeLink(link);
		Couple<ItemStack> newFrequency = link.getFrequency();
		if (newFrequency == null)
			return;

		if (link.isListening()) {
			receiversByFrequency.computeIfAbsent(newFrequency, $ -> new HashSet<>()).add(link);
		} else {
			transmittersByFrequency.computeIfAbsent(newFrequency, $ -> new HashSet<>()).add(link);
		}

		refreshChannel(oldFrequency);
		refreshChannel(newFrequency);
		setDirty();
	}

	public void removeLink(IRedstoneLinkable link) {
		if (link == null)
			return;

		Couple<ItemStack> frequency = link.getFrequency();
		if (frequency == null)
			return;

		if (link.isListening()) {
			Set<IRedstoneLinkable> set = receiversByFrequency.get(frequency);
			if (set != null) set.remove(link);
		} else {
			Set<IRedstoneLinkable> set = transmittersByFrequency.get(frequency);
			if (set != null) set.remove(link);
		}

		refreshChannel(frequency);
		setDirty();
	}

	public void refreshChannel(Couple<ItemStack> frequency) {
		if (frequency == null)
			return;

		Set<IRedstoneLinkable> transmitters = transmittersByFrequency.getOrDefault(frequency, Set.of());
		Set<IRedstoneLinkable> receivers = receiversByFrequency.getOrDefault(frequency, Set.of());

		int maxPower = 0;
		for (IRedstoneLinkable tx : transmitters) {
			maxPower = Math.max(maxPower, tx.getTransmittedStrength());
		}

		for (IRedstoneLinkable rx : receivers) {
			if (!rx.getFrequency().equals(frequency))
				continue;

			rx.setReceivedStrength(maxPower);
			if (rx instanceof RedstoneLinkBlockEntity be) {
				be.tick();
			}
		}
	}



	@Override
	public CompoundTag save(CompoundTag tag) {
		// Пока сериализация не реализована
		return tag;
	}

	private void load(CompoundTag tag) {
		// Пока десериализация не реализована
	}
}