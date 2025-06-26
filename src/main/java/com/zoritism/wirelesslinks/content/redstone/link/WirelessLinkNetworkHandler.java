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

/**
 * Обработчик сети WirelessLink — управляет связью между частотами.
 */
@Mod.EventBusSubscriber
public class WirelessLinkNetworkHandler {

    // Используем WeakHashMap, чтобы уровни автоматически удалялись при выгрузке
    private static final Map<Level, Map<Couple<Frequency>, List<IRedstoneLinkable>>> NETWORKS = new WeakHashMap<>();

    public static void addToNetwork(Level level, IRedstoneLinkable link) {
        if (level == null || link == null || link.getNetworkKey() == null)
            return;

        NETWORKS
                .computeIfAbsent(level, l -> new HashMap<>())
                .computeIfAbsent(link.getNetworkKey(), k -> new ArrayList<>())
                .add(link);

        updateNetwork(level, link.getNetworkKey());
    }

    public static void removeFromNetwork(Level level, IRedstoneLinkable link) {
        if (level == null || link == null || link.getNetworkKey() == null)
            return;

        Map<Couple<Frequency>, List<IRedstoneLinkable>> networks = NETWORKS.get(level);
        if (networks == null)
            return;

        List<IRedstoneLinkable> list = networks.get(link.getNetworkKey());
        if (list == null)
            return;

        list.removeIf(l -> l == null || l.getLocation() == null || l.getLocation().equals(link.getLocation()));

        if (list.isEmpty()) {
            networks.remove(link.getNetworkKey());
            if (networks.isEmpty())
                NETWORKS.remove(level);
        } else {
            updateNetwork(level, link.getNetworkKey());
        }
    }

    public static void updateNetwork(Level level, Couple<Frequency> key) {
        if (level == null || key == null)
            return;

        Map<Couple<Frequency>, List<IRedstoneLinkable>> networks = NETWORKS.get(level);
        if (networks == null)
            return;

        List<IRedstoneLinkable> list = networks.get(key);
        if (list == null || list.isEmpty())
            return;

        int maxPower = 0;

        try {
            // Сначала найдём максимум от всех передатчиков
            for (IRedstoneLinkable link : list) {
                if (link != null && link.isAlive() && !link.isListening())
                    maxPower = Math.max(maxPower, link.getTransmittedStrength());
            }

            // Теперь передадим мощность всем приёмникам
            for (IRedstoneLinkable link : list) {
                if (link != null && link.isAlive() && link.isListening())
                    link.setReceivedStrength(maxPower);
            }

        } catch (ConcurrentModificationException e) {
            // Может случиться при выгрузке чанков — безопасно игнорируем
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
        if (accessor instanceof Level level)
            NETWORKS.remove(level);
    }
}
