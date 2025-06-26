package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.IRedstoneLinkable;
import com.zoritism.wirelesslinks.content.redstone.link.LinkHandler;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlockEntity;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkedControllerServerHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Вызывается при получении сигнала с контроллера.
	 * Для каждой частоты находит всех receiver и выставляет им powered=true (или false).
	 * Сигнал "залипает" и держится пока не придет противоположная команда.
	 */
	public static void setReceiversPowered(Level level, List<Couple<ItemStack>> frequencies, boolean powered) {
		if (level == null || frequencies == null || frequencies.isEmpty())
			return;
		LinkHandler handler = LinkHandler.get(level);
		for (Couple<ItemStack> freq : frequencies) {
			Set<IRedstoneLinkable> receivers = getReceiversByFrequency(handler, freq);
			LOGGER.info("[LinkedControllerServerHandler][setReceiversPowered] frequency={}, receiversCount={}, powered={}", freq, receivers.size(), powered);
			for (IRedstoneLinkable rx : receivers) {
				if (rx.isListening()) {
					rx.setReceivedStrength(powered ? 15 : 0);
					if (rx instanceof RedstoneLinkBlockEntity be) {
						be.tick();
					}
				}
			}
		}
	}

	private static Set<IRedstoneLinkable> getReceiversByFrequency(LinkHandler handler, Couple<ItemStack> freq) {
		try {
			java.lang.reflect.Field field = LinkHandler.class.getDeclaredField("receiversByFrequency");
			field.setAccessible(true);
			Map<Couple<ItemStack>, Set<IRedstoneLinkable>> map = (Map<Couple<ItemStack>, Set<IRedstoneLinkable>>) field.get(handler);
			return map.getOrDefault(freq, Collections.emptySet());
		} catch (Exception e) {
			LOGGER.error("Failed to access receiversByFrequency via reflection: {}", e.toString());
			return Collections.emptySet();
		}
	}
}