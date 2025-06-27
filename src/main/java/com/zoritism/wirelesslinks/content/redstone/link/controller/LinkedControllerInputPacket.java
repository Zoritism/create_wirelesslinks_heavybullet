package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase implements com.zoritism.wirelesslinks.foundation.network.ModPackets.SimplePacketBase {

    private Collection<Integer> activatedButtons;
    private boolean press;

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
                Object lectern = getLectern(level, getLecternPos());
                handleLectern(player, lectern);
            } else {
                ItemStack heldController = getHeldController(player);
                handleItem(player, heldController);
            }
        });
        return true;
    }

    @Override
    protected void handleLectern(ServerPlayer player, Object lecternGeneric) {
        if (!(lecternGeneric instanceof LecternControllerBlockEntity lectern))
            return;
        if (lectern.isUsedBy(player)) {
            // Вместо handleItem, берём контент из лекторна, но pos для сигнала — это сам лекторн!
            ItemStack controllerStack = lectern.getController();
            Level world = player.level();
            UUID uniqueID = player.getUUID();
            BlockPos pos = lectern.getBlockPos(); // именно позиция лекторна
            if (player.isSpectator() && press)
                return;
            ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(controllerStack);
            List<Couple<ItemStack>> frequencies = new ArrayList<>();
            for (int i : activatedButtons) {
                int aIdx = i * 2;
                int bIdx = aIdx + 1;
                ItemStack a = inv.getStackInSlot(aIdx);
                ItemStack b = inv.getStackInSlot(bIdx);
                if (!a.isEmpty() || !b.isEmpty()) {
                    frequencies.add(Couple.of(a, b));
                }
            }
            LinkedControllerServerHandler.receivePressed(world, pos, uniqueID, frequencies, press);
        }
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

        // Собираем список пар ItemStack (Couple<ItemStack>) для каждой активной кнопки
        ItemStackHandler inv = LinkedControllerItem.getFrequencyInventory(heldItem);
        List<Couple<ItemStack>> frequencies = new ArrayList<>();
        for (int i : activatedButtons) {
            int aIdx = i * 2;
            int bIdx = aIdx + 1;
            ItemStack a = inv.getStackInSlot(aIdx);
            ItemStack b = inv.getStackInSlot(bIdx);
            if (!a.isEmpty() || !b.isEmpty()) {
                frequencies.add(Couple.of(a, b));
            }
        }

        LinkedControllerServerHandler.receivePressed(world, pos, uniqueID, frequencies, press);
    }

    protected LecternControllerBlockEntity getLectern(Level level, BlockPos pos) {
        if (level == null || pos == null)
            return null;
        var be = level.getBlockEntity(pos);
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