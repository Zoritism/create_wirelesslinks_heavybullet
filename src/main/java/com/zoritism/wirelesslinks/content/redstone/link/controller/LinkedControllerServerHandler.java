package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.LinkHandler;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Теперь серверная обработка работает как виртуальный transmitter в LinkHandler.
 * Сброс/установка сигнала по частоте происходит через setVirtualTransmitter/removeVirtualTransmitter.
 */
public class LinkedControllerServerHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Вызывается при получении сигнала с контроллера.
	 * Для каждой частоты регистрирует или удаляет виртуальный transmitter для игрока.
	 * Сигнал не залипает: если нет активного transmitter (контроллера или блока), сигнал сбрасывается.
	 */
	public static void setReceiversPowered(Level level, List<Couple<ItemStack>> frequencies, boolean powered, UUID playerId) {
		if (level == null || frequencies == null || frequencies.isEmpty() || playerId == null)
			return;
		LinkHandler handler = LinkHandler.get(level);
		for (Couple<ItemStack> freq : frequencies) {
			if (powered) {
				handler.setVirtualTransmitter(freq, playerId, 15);
				LOGGER.info("[LinkedControllerServerHandler][setReceiversPowered] freq={}, player={}, powered=ON", freq, playerId);
			} else {
				handler.removeVirtualTransmitter(freq, playerId);
				LOGGER.info("[LinkedControllerServerHandler][setReceiversPowered] freq={}, player={}, powered=OFF", freq, playerId);
			}
		}
	}

	/**
	 * Удаляет все виртуальные передатчики для игрока (вызывать при смерти, дисконнекте...).
	 */
	public static void removeAllReceiversFor(Level level, UUID playerId) {
		if (level == null || playerId == null)
			return;
		LinkHandler.get(level).removeAllVirtualTransmittersFor(playerId);
	}
}