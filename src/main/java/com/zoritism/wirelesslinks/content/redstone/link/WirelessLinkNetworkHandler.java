package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Обработчик сети WirelessLink — управляет связью между частотами.
 */
@Mod.EventBusSubscriber
public class WirelessLinkNetworkHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    // Используем WeakHashMap, чтобы уровни автоматически удалялись при выгрузке
    private static final Map<Level, Map<Couple<Frequency>, List<IRedstoneLinkable>>> NETWORKS = new WeakHashMap<>();

    public static void addToNetwork(Level level, IRedstoneLinkable link) {
        if (level == null || link == null || link.getNetworkKey() == null) {
            LOGGER.info("[WLNH] addToNetwork: level/link/key is null (level={}, link={}, key={})", level, link, link == null ? null : link.getNetworkKey());
            return;
        }

        LOGGER.info("[WLNH] addToNetwork: level={}, linkClass={}, linkLoc={}, key={}, isListening={}, isAlive={}",
                level.dimension().location(), link.getClass().getSimpleName(), link.getLocation(), link.getNetworkKey(), link.isListening(), link.isAlive());

        NETWORKS
                .computeIfAbsent(level, l -> new HashMap<>())
                .computeIfAbsent(link.getNetworkKey(), k -> new ArrayList<>())
                .add(link);

        LOGGER.info("[WLNH] addToNetwork: Added to network. Current network size for key {}: {}", link.getNetworkKey(), NETWORKS.get(level).get(link.getNetworkKey()).size());

        // Логируем текущее содержимое сети для ключа
        List<IRedstoneLinkable> currentList = NETWORKS.get(level).get(link.getNetworkKey());
        if (currentList != null) {
            for (IRedstoneLinkable l : currentList) {
                LOGGER.info("[WLNH] [NetworkContent] key={} linkClass={} loc={} isListening={} isAlive={}",
                        link.getNetworkKey(), l.getClass().getSimpleName(), l.getLocation(), l.isListening(), l.isAlive());
            }
        }

        updateNetwork(level, link.getNetworkKey());
    }

    public static void removeFromNetwork(Level level, IRedstoneLinkable link) {
        if (level == null || link == null || link.getNetworkKey() == null) {
            LOGGER.info("[WLNH] removeFromNetwork: level/link/key is null (level={}, link={}, key={})", level, link, link == null ? null : link.getNetworkKey());
            return;
        }

        LOGGER.info("[WLNH] removeFromNetwork: level={}, linkClass={}, linkLoc={}, key={}", level.dimension().location(), link.getClass().getSimpleName(), link.getLocation(), link.getNetworkKey());

        Map<Couple<Frequency>, List<IRedstoneLinkable>> networks = NETWORKS.get(level);
        if (networks == null) {
            LOGGER.info("[WLNH] removeFromNetwork: networks==null for level={}", level.dimension().location());
            return;
        }

        List<IRedstoneLinkable> list = networks.get(link.getNetworkKey());
        if (list == null) {
            LOGGER.info("[WLNH] removeFromNetwork: list==null for key={}", link.getNetworkKey());
            return;
        }

        int before = list.size();
        list.removeIf(l -> l == null || l.getLocation() == null || l.getLocation().equals(link.getLocation()));
        int after = list.size();

        LOGGER.info("[WLNH] removeFromNetwork: After removal, size for key {}: {} (removed {})", link.getNetworkKey(), after, before - after);

        // Логируем оставшихся участников сети
        if (after > 0) {
            for (IRedstoneLinkable l : list) {
                LOGGER.info("[WLNH] [NetworkContent] key={} linkClass={} loc={} isListening={} isAlive={}",
                        link.getNetworkKey(), l.getClass().getSimpleName(), l.getLocation(), l.isListening(), l.isAlive());
            }
        }

        if (list.isEmpty()) {
            networks.remove(link.getNetworkKey());
            LOGGER.info("[WLNH] removeFromNetwork: Removed empty network for key {}", link.getNetworkKey());
            if (networks.isEmpty()) {
                NETWORKS.remove(level);
                LOGGER.info("[WLNH] removeFromNetwork: All networks empty for level={}, removed level", level.dimension().location());
            }
        } else {
            updateNetwork(level, link.getNetworkKey());
        }
    }

    public static void updateNetwork(Level level, Couple<Frequency> key) {
        if (level == null || key == null) {
            LOGGER.info("[WLNH] updateNetwork: level or key is null (level={}, key={})", level, key);
            return;
        }

        Map<Couple<Frequency>, List<IRedstoneLinkable>> networks = NETWORKS.get(level);
        if (networks == null) {
            LOGGER.info("[WLNH] updateNetwork: networks==null for level={}", level.dimension().location());
            return;
        }

        List<IRedstoneLinkable> list = networks.get(key);
        if (list == null || list.isEmpty()) {
            LOGGER.info("[WLNH] updateNetwork: list is null or empty for key={}", key);
            return;
        }

        int maxPower = 0;
        int transmitters = 0;
        int receivers = 0;

        // Логируем всю сеть по ключу
        for (IRedstoneLinkable link : list) {
            LOGGER.info("[WLNH] [UpdateNetwork] key={} linkClass={} loc={} isListening={} isAlive={} transmittedStrength={}",
                    key, link.getClass().getSimpleName(), link.getLocation(), link.isListening(), link.isAlive(), link.getTransmittedStrength());
        }

        try {
            // Сначала найдём максимум от всех передатчиков
            for (IRedstoneLinkable link : list) {
                if (link != null && link.isAlive() && !link.isListening()) {
                    int power = link.getTransmittedStrength();
                    LOGGER.info("[WLNH] updateNetwork: TRANSMITTER at {} key={} power={}", link.getLocation(), key, power);
                    maxPower = Math.max(maxPower, power);
                    transmitters++;
                }
            }

            // Теперь передадим мощность всем приёмникам
            for (IRedstoneLinkable link : list) {
                if (link != null && link.isAlive() && link.isListening()) {
                    receivers++;
                    link.setReceivedStrength(maxPower);
                    LOGGER.info("[WLNH] updateNetwork: RECEIVER at {} key={} setReceivedStrength={}", link.getLocation(), key, maxPower);
                }
            }

            LOGGER.info("[WLNH] updateNetwork: Summary for key={}: transmitters={}, receivers={}, maxPower={}", key, transmitters, receivers, maxPower);

        } catch (ConcurrentModificationException e) {
            LOGGER.warn("[WLNH] updateNetwork: ConcurrentModificationException caught, likely during chunk unload.");
        }
    }

    public static List<IRedstoneLinkable> getNetworkOf(Level level, IRedstoneLinkable link) {
        if (level == null || link == null || link.getNetworkKey() == null)
            return Collections.emptyList();

        Map<Couple<Frequency>, List<IRedstoneLinkable>> map = NETWORKS.get(level);
        if (map == null)
            return Collections.emptyList();

        return map.getOrDefault(link.getNetworkKey(), Collections.emptyList());
    }

    // Автоматическая очистка при выгрузке уровня
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        LevelAccessor accessor = event.getLevel();
        if (accessor instanceof Level level) {
            LOGGER.info("[WLNH] onWorldUnload: Unloading level={}", level.dimension().location());
            NETWORKS.remove(level);
        }
    }
}