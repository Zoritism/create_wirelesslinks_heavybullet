package com.zoritism.wirelesslinks.foundation.network;

import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.foundation.gui.menu.ClearMenuPacket;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerInputPacket;

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

public enum ModPackets {

    // Пример регистрации своего пакета (добавляй сюда другие пакеты по мере необходимости)
    CLEAR_CONTAINER(ClearMenuPacket.class, ClearMenuPacket::new, NetworkDirection.PLAY_TO_SERVER),

    // Новый пакет контроллера!
    LINKED_CONTROLLER_INPUT(LinkedControllerInputPacket.class, LinkedControllerInputPacket::new, NetworkDirection.PLAY_TO_SERVER);

    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(WirelessLinksMod.MODID, "main");
    public static final int NETWORK_VERSION = 1;
    public static final String NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);
    private static SimpleChannel channel;

    private PacketType<?> packetType;

    <T> ModPackets(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
        packetType = new PacketType<>(type, factory, direction);
    }

    public static void registerPackets() {
        channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
                .serverAcceptedVersions(NETWORK_VERSION_STR::equals)
                .clientAcceptedVersions(NETWORK_VERSION_STR::equals)
                .networkProtocolVersion(() -> NETWORK_VERSION_STR)
                .simpleChannel();

        for (ModPackets packet : values())
            packet.packetType.register();
    }

    public static SimpleChannel getChannel() {
        return channel;
    }

    public static void sendToNear(Level world, BlockPos pos, int range, Object message) {
        getChannel().send(
                PacketDistributor.NEAR.with(TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), range, world.dimension())),
                message
        );
    }

    private static class PacketType<T> {
        private static int index = 0;

        private BiConsumer<T, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, T> decoder;
        private BiConsumer<T, Supplier<NetworkEvent.Context>> handler;
        private Class<T> type;
        private NetworkDirection direction;

        @SuppressWarnings("unchecked")
        private PacketType(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
            // Если твои пакеты реализуют метод write(FriendlyByteBuf), используем его
            this.encoder = (pkt, buf) -> {
                try {
                    type.getMethod("write", FriendlyByteBuf.class).invoke(pkt, buf);
                } catch (Exception e) {
                    throw new RuntimeException("Packet " + type.getName() + " must have method: void write(FriendlyByteBuf)", e);
                }
            };
            this.decoder = factory;
            this.handler = (packet, contextSupplier) -> {
                try {
                    // handle должен быть: public void handle(Supplier<NetworkEvent.Context> ctx)
                    type.getMethod("handle", Supplier.class).invoke(packet, contextSupplier);
                } catch (Exception e) {
                    throw new RuntimeException("Packet " + type.getName() + " must have method: void handle(Supplier<NetworkEvent.Context>)", e);
                }
            };
            this.type = type;
            this.direction = direction;
        }

        private void register() {
            getChannel().messageBuilder(type, index++, direction)
                    .encoder(encoder)
                    .decoder(decoder)
                    .consumerNetworkThread(handler)
                    .add();
        }
    }
}