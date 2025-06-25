package com.zoritism.wirelesslinks.foundation.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет для очистки ghost-слотов меню. Аналог Create.
 */
public class ClearMenuPacket {

    public ClearMenuPacket() {}

    public ClearMenuPacket(FriendlyByteBuf buffer) {}

    public void write(FriendlyByteBuf buffer) {}

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null)
                return;
            if (!(player.containerMenu instanceof IClearableMenu clearable))
                return;
            clearable.clearContents();
            // Если требуется, можно добавить: ((MenuBase<?>) clearable).saveData(...)
        });
        context.get().setPacketHandled(true);
    }
}