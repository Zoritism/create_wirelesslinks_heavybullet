package com.zoritism.wirelesslinks.foundation.network;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerInputPacket;
import com.zoritism.wirelesslinks.foundation.gui.menu.ClearMenuPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Аналог AllPackets из Create.
 * Все ваши пакеты должны реализовывать интерфейс ModPackets.SimplePacketBase!
 */
public enum ModPackets {

    CLEAR_CONTAINER(ClearMenuPacket.class, ClearMenuPacket::new, NetworkDirection.PLAY_TO_SERVER),
    LINKED_CONTROLLER_INPUT(LinkedControllerInputPacket.class, LinkedControllerInputPacket::new, NetworkDirection.PLAY_TO_SERVER);

    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(WirelessLinksMod.MODID, "main");
    public static final int NETWORK_VERSION = 1;
    public static final String NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);
    private static SimpleChannel channel;

    private static final Logger LOGGER = LogManager.getLogger();

    private final PacketType<?> packetType;

    <T extends SimplePacketBase> ModPackets(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
        this.packetType = new PacketType<>(type, factory, direction);
    }

    public static void registerPackets() {
        LOGGER.info("[PACKET] Registering channel and all packets...");
        channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
                .serverAcceptedVersions(NETWORK_VERSION_STR::equals)
                .clientAcceptedVersions(NETWORK_VERSION_STR::equals)
                .networkProtocolVersion(() -> NETWORK_VERSION_STR)
                .simpleChannel();

        for (ModPackets packet : values()) {
            LOGGER.info("[PACKET] Registering packet: {}", packet.packetType.type.getSimpleName());
            packet.packetType.register();
        }
        LOGGER.info("[PACKET] All packets registered.");
    }

    public static SimpleChannel getChannel() {
        if (channel == null) {
            LOGGER.error("[PACKET] getChannel() called before registration! Channel is null.");
        }
        return channel;
    }

    public static void sendToNear(Level world, BlockPos pos, int range, Object message) {
        LOGGER.info("[PACKET] sendToNear: pos={}, range={}, message={}", pos, range, message.getClass().getSimpleName());
        getChannel().send(
                PacketDistributor.NEAR.with(TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), range, world.dimension())),
                message
        );
    }

    // ------------------- Аналог PacketType из Create -------------------
    private static class PacketType<T extends SimplePacketBase> {
        private static int index = 0;

        private final BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final BiConsumer<T, Supplier<Context>> handler;
        private final Class<T> type;
        private final NetworkDirection direction;

        private PacketType(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
            this.encoder = (packet, buf) -> {
                LOGGER.info("[PACKET] Encoding packet: {}", type.getSimpleName());
                packet.write(buf);
            };
            this.decoder = (buf) -> {
                LOGGER.info("[PACKET] Decoding packet: {}", type.getSimpleName());
                return factory.apply(buf);
            };
            this.handler = (packet, contextSupplier) -> {
                Context context = contextSupplier.get();
                LOGGER.info("[PACKET] Handling packet: {} on {}", type.getSimpleName(), context.getDirection());
                if (packet.handle(context)) {
                    context.setPacketHandled(true);
                    LOGGER.info("[PACKET] Packet {} marked as handled.", type.getSimpleName());
                }
            };
            this.type = type;
            this.direction = direction;
        }

        private void register() {
            LOGGER.info("[PACKET] Registering packet on channel: {}, type: {}, direction: {}", CHANNEL_NAME, type.getSimpleName(), direction);
            getChannel().messageBuilder(type, index++, direction)
                    .encoder(encoder)
                    .decoder(decoder)
                    .consumerNetworkThread(handler)
                    .add();
        }
    }

    // ------------------- Интерфейс для всех пакетов -------------------
    public interface SimplePacketBase {
        void write(FriendlyByteBuf buf);
        /**
         * Возвращает true если пакет был обработан и должен быть помечен обработанным.
         * Обычно: context.enqueueWork(...); return true;
         */
        boolean handle(Context context);
    }
}
