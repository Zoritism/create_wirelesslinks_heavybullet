package com.zoritism.wirelesslinks.content.redstone.link.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Пакет для передачи состояния нажатых кнопок контроллера.
 */
public class LinkedControllerInputPacket extends LinkedControllerPacketBase {

    private final Collection<Integer> activatedButtons;
    private final boolean press;

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        super(lecternPos);
        this.activatedButtons = activatedButtons;
        this.press = press;
    }

    public LinkedControllerInputPacket(FriendlyByteBuf buffer) {
        super(buffer);
        this.press = buffer.readBoolean();
        int size = buffer.readVarInt();
        this.activatedButtons = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            this.activatedButtons.add(buffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        super.write(buffer);
        buffer.writeBoolean(press);
        buffer.writeVarInt(activatedButtons.size());
        activatedButtons.forEach(buffer::writeVarInt);
    }

    @Override
    protected void handleLectern(ServerPlayer player, Object lecternObj) {
        // Если реализуешь поддержку LecternControllerBlockEntity — замени Object на нужный тип и реализуй метод
        // if (lecternObj instanceof LecternControllerBlockEntity lectern && lectern.isUsedBy(player))
        //     handleItem(player, lectern.getController());
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        Level world = player.getCommandSenderWorld();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press)
            return;

        LinkedControllerServerHandler.receivePressed(
                world,
                pos,
                uniqueID,
                activatedButtons.stream()
                        .map(i -> LinkedControllerItem.slotToFrequency(heldItem, i))
                        .collect(Collectors.toList()),
                press
        );
    }
}