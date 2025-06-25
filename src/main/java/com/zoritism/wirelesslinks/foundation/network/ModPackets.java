package com.zoritism.wirelesslinks.foundation.network;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerInputPacket;
import com.zoritism.wirelesslinks.foundation.gui.menu.ClearMenuPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Регистрация и отправка сетевых пакетов для WirelessLinks.
 * Выполнено по образцу Create: каждый пакет регистрируется с encoder/decoder/handler.
 */
public class ModPackets {

    public static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(WirelessLinksMod.MODID, "main");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int index = 0;

    public static void registerPackets() {
        // Очистка контейнера (пример)
        CHANNEL.registerMessage(index++,
                ClearMenuPacket.class,
                (pkt, buf) -> pkt.write(buf),
                ClearMenuPacket::new,
                (pkt, ctxSupplier) -> {
                    pkt.handle(ctxSupplier);
                },
                NetworkDirection.PLAY_TO_SERVER
        );

        // Пакет контроллера (главное!)
        CHANNEL.registerMessage(index++,
                LinkedControllerInputPacket.class,
                (pkt, buf) -> pkt.write(buf),
                LinkedControllerInputPacket::new,
                (pkt, ctxSupplier) -> {
                    pkt.handle(ctxSupplier);
                },
                NetworkDirection.PLAY_TO_SERVER
        );
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }

    /** Утилита для отправки пакета "рядом" с позицией (аналогично Create) */
    public static void sendToNear(Level world, BlockPos pos, int range, Object message) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), range, world.dimension())),
                message
        );
    }
}