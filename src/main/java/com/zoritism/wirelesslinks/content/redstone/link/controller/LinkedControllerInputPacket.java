package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.FrequencyPair;
import com.zoritism.wirelesslinks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Пакет для передачи нажатий с Linked Controller с клиента на сервер.
 * Адаптация под проект WirelessLinks (на основе Create).
 */
public class LinkedControllerInputPacket {

    private final Collection<Integer> activatedButtons;
    private final boolean press;
    private final BlockPos lecternPos;

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        this.activatedButtons = new ArrayList<>(activatedButtons);
        this.press = press;
        this.lecternPos = lecternPos;
    }

    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    public LinkedControllerInputPacket(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            this.lecternPos = buffer.readBlockPos();
        } else {
            this.lecternPos = null;
        }
        this.press = buffer.readBoolean();
        int size = buffer.readVarInt();
        this.activatedButtons = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            activatedButtons.add(buffer.readVarInt());
        }
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(lecternPos != null);
        if (lecternPos != null) {
            buffer.writeBlockPos(lecternPos);
        }
        buffer.writeBoolean(press);
        buffer.writeVarInt(activatedButtons.size());
        for (int i : activatedButtons) {
            buffer.writeVarInt(i);
        }
    }

    /**
     * Вызывается на сервере после получения пакета.
     */
    public void handle(ServerPlayer player) {
        // Если lecternPos != null, ищем Lectern и предмет, иначе используем предмет из руки
        if (lecternPos != null) {
            // TODO: логика поиска Lectern и получения контроллера, если потребуется
            // Пока просто fallback на предмет из руки
        }

        ItemStack heldItem = getHeldController(player);
        if (heldItem == null || !heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return;

        Level world = player.getCommandSenderWorld();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press)
            return;

        // Собираем частотные пары (FrequencyPair) для переданных индексов кнопок
        List<FrequencyPair> pairs = activatedButtons.stream()
                .map(i -> LinkedControllerItem.slotToFrequency(heldItem, i))
                .collect(Collectors.toList());

        // Вызываем серверный обработчик
        LinkedControllerServerHandler.receivePressed(
                world, pos, uniqueID,
                pairs.stream().map(FrequencyPair::toCouple).collect(Collectors.toList()),
                press
        );
    }

    private ItemStack getHeldController(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return heldItem;
        heldItem = player.getOffhandItem();
        if (heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return heldItem;
        return null;
    }
}