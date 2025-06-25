package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.FrequencyPair;
import com.zoritism.wirelesslinks.foundation.network.ModPackets;
import com.zoritism.wirelesslinks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.stream.Collectors;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase implements ModPackets.SimplePacketBase {

    private Collection<Integer> activatedButtons;
    private boolean press;

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        super(lecternPos);
        this.activatedButtons = new ArrayList<>(activatedButtons);
        this.press = press;
    }

    public LinkedControllerInputPacket(FriendlyByteBuf buffer) {
        super(buffer);
        activatedButtons = new ArrayList<>();
        press = buffer.readBoolean();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++)
            activatedButtons.add(buffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        super.write(buffer);
        buffer.writeBoolean(press);
        buffer.writeVarInt(activatedButtons.size());
        activatedButtons.forEach(buffer::writeVarInt);
    }

    @Override
    public boolean handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;
            if (hasLectern()) {
                Level level = player.level();
                LecternControllerBlockEntity lectern = getLectern(level, getLecternPos());
                handleLectern(player, lectern);
            } else {
                handleItem(player, getHeldController(player));
            }
        });
        return true;
    }

    @Override
    protected void handleLectern(ServerPlayer player, Object lecternGeneric) {
        if (!(lecternGeneric instanceof LecternControllerBlockEntity lectern))
            return;
        if (lectern.isUsedBy(player))
            handleItem(player, lectern.getController());
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        if (heldItem == null || !heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return;

        Level world = player.level();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press)
            return;

        List<FrequencyPair> pairs = activatedButtons.stream()
                .map(i -> LinkedControllerItem.slotToFrequency(heldItem, i))
                .collect(Collectors.toList());

        LinkedControllerServerHandler.receivePressed(
                world, pos, uniqueID,
                pairs.stream().map(FrequencyPair::toCouple).collect(Collectors.toList()),
                press
        );
    }

    protected LecternControllerBlockEntity getLectern(Level level, BlockPos pos) {
        if (level == null || pos == null)
            return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LecternControllerBlockEntity l)
            return l;
        return null;
    }

    protected boolean hasLectern() {
        return getLecternPos() != null;
    }

    protected BlockPos getLecternPos() {
        return lecternPos;
    }

    protected ItemStack getHeldController(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return heldItem;
        heldItem = player.getOffhandItem();
        if (heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return heldItem;
        return null;
    }
}