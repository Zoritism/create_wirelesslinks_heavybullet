package com.zoritism.wirelesslinks.content.redstone.link.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Базовый класс для пакетов контроллера.
 * Адаптирован под wirelesslinks: без поддержки LecternControllerBlockEntity.
 */
public abstract class LinkedControllerPacketBase {

    private BlockPos lecternPos;

    public LinkedControllerPacketBase(BlockPos lecternPos) {
        this.lecternPos = lecternPos;
    }

    public LinkedControllerPacketBase(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            lecternPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        }
    }

    protected boolean inLectern() {
        return lecternPos != null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(inLectern());
        if (inLectern()) {
            buffer.writeInt(lecternPos.getX());
            buffer.writeInt(lecternPos.getY());
            buffer.writeInt(lecternPos.getZ());
        }
    }

    /**
     * Обработка пакета на сервере.
     * Находит игрока-отправителя и вызывает handleItem().
     */
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null)
                return;

            // В твоём проекте LecternControllerBlockEntity не используется, оставляем только работу с предметом
            ItemStack controller = player.getMainHandItem();
            if (!isLinkedController(controller)) {
                controller = player.getOffhandItem();
                if (!isLinkedController(controller))
                    return;
            }
            handleItem(player, controller);
        });
        return true;
    }

    /**
     * Проверяет, является ли предмет контроллером.
     * Здесь должен быть твой способ узнать контроллер (можно заменить на проверку по ModItems.LINKED_CONTROLLER)
     */
    protected boolean isLinkedController(ItemStack stack) {
        // Пример: return stack.getItem() == ModItems.LINKED_CONTROLLER.get();
        // Заменить на свою проверку!
        return stack != null && stack.getItem().getDescriptionId().contains("linked_controller");
    }

    protected abstract void handleItem(ServerPlayer player, ItemStack heldItem);
    protected abstract void handleLectern(ServerPlayer player, Object lecternObj);
}