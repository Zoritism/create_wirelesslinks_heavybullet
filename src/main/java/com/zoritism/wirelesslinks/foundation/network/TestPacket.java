package com.zoritism.wirelesslinks.foundation.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestPacket implements ModPackets.SimplePacketBase {
    private static final Logger LOGGER = LogManager.getLogger();

    private int testValue;

    public TestPacket(int testValue) {
        this.testValue = testValue;
        LOGGER.info("[TESTPACKET] Created on client, value={}", testValue);
    }

    public TestPacket(FriendlyByteBuf buf) {
        this.testValue = buf.readInt();
        LOGGER.info("[TESTPACKET] Deserialized on {}: value={}", Thread.currentThread().getName(), testValue);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(testValue);
        LOGGER.info("[TESTPACKET] Serialized: value={}", testValue);
    }

    @Override
    public boolean handle(NetworkEvent.Context ctx) {
        LOGGER.info("[TESTPACKET] handle called on {}: value={}", Thread.currentThread().getName(), testValue);
        ctx.enqueueWork(() -> {
            LOGGER.info("[TESTPACKET] handle enqueueWork on {}: value={}", Thread.currentThread().getName(), testValue);
        });
        return true;
    }
}