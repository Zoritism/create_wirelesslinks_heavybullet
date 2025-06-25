package com.zoritism.wirelesslinks.foundation.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.zoritism.wirelesslinks.foundation.network.ModPackets;

public class ClearMenuPacket implements ModPackets.SimplePacketBase {

    public ClearMenuPacket() {}

    public ClearMenuPacket(FriendlyByteBuf buffer) {}

    @Override
    public void write(FriendlyByteBuf buffer) {
        // Нет данных для записи
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null)
                return;
            if (!(player.containerMenu instanceof IClearableMenu clearable))
                return;
            clearable.clearContents();
            // Можно добавить сохранение: ((MenuBase<?>) clearable).saveData(...)
        });
        return true;
    }
}