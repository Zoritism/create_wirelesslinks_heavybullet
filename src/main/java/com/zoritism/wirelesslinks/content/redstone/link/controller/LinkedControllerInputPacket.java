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

import java.util.*;
import java.util.UUID;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase implements com.zoritism.wirelesslinks.foundation.network.ModPackets.SimplePacketBase {

    private static final Logger LOGGER = LogManager.getLogger();
    private Collection<Integer> activatedButtons;
    private boolean press;

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        super(lecternPos);
        this.activatedButtons = new ArrayList<>(activatedButtons);
        this.press = press;
        LOGGER.info("[PACKET] Created on client: activatedButtons={}, press={}, lecternPos={}", activatedButtons, press, lecternPos);
    }

    public LinkedControllerInputPacket(FriendlyByteBuf buffer) {
        super(buffer);
        activatedButtons = new ArrayList<>();
        press = buffer.readBoolean();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++)
            activatedButtons.add(buffer.readVarInt());
        LOGGER.info("[PACKET] Deserialized: activatedButtons={}, press={}, lecternPos={}", activatedButtons, press, lecternPos);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        super.write(buffer);
        buffer.writeBoolean(press);
        buffer.writeVarInt(activatedButtons.size());
        activatedButtons.forEach(buffer::writeVarInt);
        LOGGER.info("[PACKET] Serialized: activatedButtons={}, press={}, lecternPos={}", activatedButtons, press, lecternPos);
    }

    @Override
    public boolean handle(NetworkEvent.Context ctx) {
        LOGGER.info("[PACKET] handle called. Direction: {}, Sender: {}", ctx.getDirection(), ctx.getSender());
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            LOGGER.info("[PACKET] handle enqueueWork: player={}", player);
            if (player == null)
                return;
            if (hasLectern()) {
                Level level = player.level();
                Object lectern = getLectern(level, getLecternPos());
                LOGGER.info("[PACKET] handle: hasLectern, level={}, lecternPos={}, lectern={}", level, getLecternPos(), lectern);
                handleLectern(player, lectern);
            } else {
                ItemStack heldController = getHeldController(player);
                LOGGER.info("[PACKET] handle: no lectern, heldController={}", heldController);
                handleItem(player, heldController);
            }
        });
        return true;
    }

    @Override
    protected void handleLectern(ServerPlayer player, Object lecternGeneric) {
        LOGGER.info("[PACKET] handleLectern: player={}, lecternGeneric={}", player, lecternGeneric);
        if (!(lecternGeneric instanceof LecternControllerBlockEntity lectern))
            return;
        if (lectern.isUsedBy(player)) {
            LOGGER.info("[PACKET] handleLectern: lectern is used by player, calling handleItem with controller={}", lectern.getController());
            handleItem(player, lectern.getController());
        }
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {
        LOGGER.info("[PACKET] handleItem: player={}, heldItem={}", player, heldItem);
        if (heldItem == null || !heldItem.is(ModItems.LINKED_CONTROLLER.get())) {
            LOGGER.info("[PACKET] handleItem: heldItem is null or not a linked controller, skipping");
            return;
        }

        Level world = player.level();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press) {
            LOGGER.info("[PACKET] handleItem: player is spectator and press=true, skipping");
            return;
        }

        // Используем оригинальные ItemStack из инвентаря контроллера
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

        LOGGER.info("[PACKET] handleItem: world={}, pos={}, uuid={}, frequencies={}, press={}", world, pos, uniqueID, frequencies, press);

        // Новый sticky-сигнал: регистрируем виртуальный transmitter (или убираем)
        LinkedControllerServerHandler.setReceiversPowered(
                world, frequencies, press, uniqueID
        );
    }

    protected LecternControllerBlockEntity getLectern(Level level, BlockPos pos) {
        if (level == null || pos == null)
            return null;
        var be = level.getBlockEntity(pos);
        LOGGER.info("[PACKET] getLectern: level={}, pos={}, blockEntity={}", level, pos, be);
        if (be instanceof LecternControllerBlockEntity l)
            return l;
        return null;
    }

    protected boolean hasLectern() {
        boolean has = getLecternPos() != null;
        LOGGER.info("[PACKET] hasLectern: {}", has);
        return has;
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