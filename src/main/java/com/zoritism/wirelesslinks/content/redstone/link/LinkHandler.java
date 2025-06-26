package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Менеджер сетей Redstone Link с поддержкой частот.
 */
public class LinkHandler extends SavedData {
	private static final String NAME = "wirelesslinks_data";
	private static LinkHandler clientInstance;

	private final Map<Couple<ItemStack>, Set<IRedstoneLinkable>> transmittersByFrequency = new HashMap<>();
	private final Map<Couple<ItemStack>, Set<IRedstoneLinkable>> receiversByFrequency = new HashMap<>();

	private static final Logger LOGGER = LogManager.getLogger();

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

	public void updateLink(IRedstoneLinkable link) {
		if (link == null)
			return;

		Couple<ItemStack> newFrequency = link.getFrequency();
		if (newFrequency == null)
			return;

		// Удаляем линк только из всех частот если он уже где-то есть
		removeLinkFromAllFrequencies(link);

		if (link.isListening()) {
			receiversByFrequency.computeIfAbsent(newFrequency, $ -> new HashSet<>()).add(link);
			LOGGER.info("[LinkHandler][updateLink] Registered receiver: {} at {} for frequency {}", link.getClass().getSimpleName(), link.getLocation(), newFrequency);
		} else {
			transmittersByFrequency.computeIfAbsent(newFrequency, $ -> new HashSet<>()).add(link);
			LOGGER.info("[LinkHandler][updateLink] Registered transmitter: {} at {} for frequency {}", link.getClass().getSimpleName(), link.getLocation(), newFrequency);
		}

		refreshChannel(newFrequency);
		setDirty();
	}

	/** Удаляет линк из всех частот (чтобы не было дублей при смене) */
	private void removeLinkFromAllFrequencies(IRedstoneLinkable link) {
		for (Set<IRedstoneLinkable> set : transmittersByFrequency.values()) {
			set.remove(link);
		}
		for (Set<IRedstoneLinkable> set : receiversByFrequency.values()) {
			set.remove(link);
		}
	}

	public void removeLink(IRedstoneLinkable link) {
		if (link == null)
			return;

		Couple<ItemStack> frequency = link.getFrequency();
		if (frequency == null)
			return;

		if (link.isListening()) {
			Set<IRedstoneLinkable> set = receiversByFrequency.get(frequency);
			if (set != null) {
				set.remove(link);
				LOGGER.info("[LinkHandler][removeLink] Removed receiver: {} at {} for frequency {}", link.getClass().getSimpleName(), link.getLocation(), frequency);
			}
		} else {
			Set<IRedstoneLinkable> set = transmittersByFrequency.get(frequency);
			if (set != null) {
				set.remove(link);
				LOGGER.info("[LinkHandler][removeLink] Removed transmitter: {} at {} for frequency {}", link.getClass().getSimpleName(), link.getLocation(), frequency);
			}
		}

		refreshChannel(frequency);
		setDirty();
	}

	public void refreshChannel(Couple<ItemStack> frequency) {
		if (frequency == null)
			return;

		Set<IRedstoneLinkable> transmitters = transmittersByFrequency.getOrDefault(frequency, Set.of());
		Set<IRedstoneLinkable> receivers = receiversByFrequency.getOrDefault(frequency, Set.of());

		LOGGER.info("[LinkHandler][refreshChannel] frequency={}", frequency);
		LOGGER.info("  transmitters count: {}", transmitters.size());
		for (IRedstoneLinkable tx : transmitters) {
			LOGGER.info("    [TX] {} at {} strength={}", tx.getClass().getSimpleName(), tx.getLocation(), tx.getTransmittedStrength());
		}
		LOGGER.info("  receivers count: {}", receivers.size());
		for (IRedstoneLinkable rx : receivers) {
			LOGGER.info("    [RX] {} at {} freq={} (equals? {})",
					rx.getClass().getSimpleName(), rx.getLocation(), rx.getFrequency(), rx.getFrequency().equals(frequency));
		}

		int maxPower = 0;
		for (IRedstoneLinkable tx : transmitters) {
			maxPower = Math.max(maxPower, tx.getTransmittedStrength());
		}

		for (IRedstoneLinkable rx : receivers) {
			if (!rx.getFrequency().equals(frequency)) {
				LOGGER.info("    [RX] {} at {} freq mismatch: rx={}, expected={}", rx.getClass().getSimpleName(), rx.getLocation(), rx.getFrequency(), frequency);
				continue;
			}
			LOGGER.info("    [RX] {} at {} setReceivedStrength({})", rx.getClass().getSimpleName(), rx.getLocation(), maxPower);
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