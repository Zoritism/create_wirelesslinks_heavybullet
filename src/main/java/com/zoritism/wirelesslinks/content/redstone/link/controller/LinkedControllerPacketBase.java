package com.zoritism.wirelesslinks.content.redstone.link.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Базовый класс для пакетов Linked Controller, по образцу Create.
 * Обеспечивает передачу позиции лекторна (если есть) и делегирует обработку.
 */
public abstract class LinkedControllerPacketBase {

    // Доступно наследникам, как в Create
    protected BlockPos lecternPos;

    public LinkedControllerPacketBase(BlockPos lecternPos) {
        this.lecternPos = lecternPos;
    }

    public LinkedControllerPacketBase(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            int x = buffer.readInt();
            int y = buffer.readInt();
            int z = buffer.readInt();
            lecternPos = new BlockPos(x, y, z);
        }
    }

    protected boolean hasLectern() {
        return lecternPos != null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(hasLectern());
        if (hasLectern()) {
            buffer.writeInt(lecternPos.getX());
            buffer.writeInt(lecternPos.getY());
            buffer.writeInt(lecternPos.getZ());
        }
    }

    /**
     * Обработка пакета на сервере.
     * Если указан lecternPos — пробуем получить LecternControllerBlockEntity и вызвать handleLectern.
     * Иначе ищем Linked Controller у игрока и вызываем handleItem.
     */
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null)
                return;
            if (hasLectern()) {
                // Получаем LecternControllerBlockEntity по позиции, если в будущем появится
                Object lectern = getLecternFromContext(player);
                handleLectern(player, lectern);
            } else {
                handleItem(player, getHeldController(player));
            }
        });
        return true;
    }

    /**
     * Получить LecternControllerBlockEntity или null, если не реализовано.
     * Можно override-ить в наследнике.
     */
    protected Object getLecternFromContext(ServerPlayer player) {
        // Если появится класс LecternControllerBlockEntity, здесь получите его по lecternPos
        return null;
    }

    /**
     * Получить Linked Controller из рук игрока (main или offhand)
     */
    protected ItemStack getHeldController(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (isLinkedController(main))
            return main;
        if (isLinkedController(off))
            return off;
        return ItemStack.EMPTY;
    }

    /**
     * Проверяет, является ли предмет Linked Controller'ом (по id)
     */
    protected boolean isLinkedController(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getDescriptionId().contains("linked_controller");
    }

    /**
     * Реализация обработки для предмета (override в потомке)
     */
    protected abstract void handleItem(ServerPlayer player, ItemStack heldItem);

    /**
     * Реализация обработки для лекторна (override в потомке)
     */
    protected abstract void handleLectern(ServerPlayer player, Object lecternObj);
}