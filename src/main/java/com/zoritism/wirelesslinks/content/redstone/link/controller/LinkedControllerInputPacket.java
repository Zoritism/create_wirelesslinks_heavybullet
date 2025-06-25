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
 * Совместимо с системой регистрации пакетов WirelessLinks & Create.
 */
public class LinkedControllerInputPacket {

    private final Collection<Integer> activatedButtons;
    private final boolean press;
    private final BlockPos lecternPos;

    // Основной конструктор для отправки
    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press, BlockPos lecternPos) {
        this.activatedButtons = new ArrayList<>(activatedButtons);
        this.press = press;
        this.lecternPos = lecternPos;
    }

    // Вспомогательный конструктор (без lectern)
    public LinkedControllerInputPacket(Collection<Integer> activatedButtons, boolean press) {
        this(activatedButtons, press, null);
    }

    // Конструктор для чтения с сети
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

    // Сериализация
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
     * Обработчик — вызывается на сервере из регистратора пакетов.
     * (ctx.enqueueWork(() -> packet.handle(ctx.getSender()));)
     */
    public void handle(ServerPlayer player) {
        if (player == null)
            return;

        // Если lecternPos != null, можно реализовать особую логику для lectern (как в Create)
        // Сейчас просто используем контроллер из руки игрока
        ItemStack heldItem = getHeldController(player);
        if (heldItem == null || !heldItem.is(ModItems.LINKED_CONTROLLER.get()))
            return;

        Level world = player.level();
        UUID uniqueID = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (player.isSpectator() && press)
            return;

        // Собираем частотные пары для переданных индексов кнопок
        List<FrequencyPair> pairs = activatedButtons.stream()
                .map(i -> LinkedControllerItem.slotToFrequency(heldItem, i))
                .collect(Collectors.toList());

        // Передаём на серверную логику
        LinkedControllerServerHandler.receivePressed(
                world, pos, uniqueID,
                pairs.stream().map(FrequencyPair::toCouple).collect(Collectors.toList()),
                press
        );
    }

    // Вспомогательный метод: возвращает контроллер из рук игрока
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