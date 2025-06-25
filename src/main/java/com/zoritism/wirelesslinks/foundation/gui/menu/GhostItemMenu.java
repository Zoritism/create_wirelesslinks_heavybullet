package com.zoritism.wirelesslinks.foundation.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Абстрактный контейнер для ghost-инвентарей (по мотивам Create).
 * Позволяет реализовывать fake-слоты, которые только визуально отображают предметы.
 * Наследуйся от него для реализации ghost-меню (например, LinkedControllerMenu).
 *
 * Исправлено: теперь ghost-слоты сохраняют исходное количество предметов (count),
 * что позволяет использовать их для частот, зависящих от количества (например, как в Redstone Link).
 */
public abstract class GhostItemMenu<T> extends MenuBase<T> implements IClearableMenu {

    public ItemStackHandler ghostInventory;

    protected GhostItemMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    protected GhostItemMenu(MenuType<?> type, int id, Inventory inv, T contentHolder) {
        super(type, id, inv, contentHolder);
    }

    /**
     * Реализуй — возвращает handler для ghost-инвентаря.
     */
    protected abstract ItemStackHandler createGhostInventory();

    /**
     * Разрешать ли одинаковые предметы в ghost-слотах.
     */
    protected abstract boolean allowRepeats();

    @Override
    protected void initAndReadInventory(T contentHolder) {
        ghostInventory = createGhostInventory();
    }

    @Override
    public void clearContents() {
        for (int i = 0; i < ghostInventory.getSlots(); i++)
            ghostInventory.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public boolean canDragTo(Slot slotIn) {
        // Если разрешены повторы — можно перетаскивать и в ghost-слоты.
        if (allowRepeats())
            return true;
        return slotIn.container == playerInventory;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        // Первые 36 — инвентарь игрока, обрабатывает MenuBase/AbstractContainerMenu
        if (slotId < 36) {
            super.clicked(slotId, dragType, clickTypeIn, player);
            return;
        }
        if (clickTypeIn == ClickType.THROW)
            return;

        ItemStack held = getCarried();
        int slot = slotId - 36;
        if (clickTypeIn == ClickType.CLONE) {
            if (player.isCreative() && held.isEmpty()) {
                ItemStack stackInSlot = ghostInventory.getStackInSlot(slot).copy();
                stackInSlot.setCount(stackInSlot.getMaxStackSize());
                setCarried(stackInSlot);
                return;
            }
            return;
        }

        ItemStack insert;
        if (held.isEmpty()) {
            insert = ItemStack.EMPTY;
        } else {
            insert = held.copy();
            // Исправлено: Больше не нормализуем count к 1, сохраняем оригинальный размер стака
            // insert.setCount(1); // удалено!
        }
        ghostInventory.setStackInSlot(slot, insert);
        getSlot(slotId).setChanged();
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        // Запрещаем любые автоматические перемещения между слотами
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        // shift-клик по инвентарю игрока -> пытаемся вставить в ghost-слоты
        // shift-клик по ghost-слоту -> очищаем его
        if (index < 36) {
            ItemStack stackToInsert = playerInventory.getItem(index);
            for (int i = 0; i < ghostInventory.getSlots(); i++) {
                ItemStack stack = ghostInventory.getStackInSlot(i);
                if (!allowRepeats() && ItemHandlerHelper.canItemStacksStack(stack, stackToInsert))
                    break;
                if (stack.isEmpty()) {
                    ItemStack copy = stackToInsert.copy();
                    // Исправлено: сохраняем оригинальное количество предметов
                    // copy.setCount(1); // удалено!
                    ghostInventory.insertItem(i, copy, false);
                    getSlot(i + 36).setChanged();
                    break;
                }
            }
        } else {
            ghostInventory.extractItem(index - 36, 1, false);
            getSlot(index).setChanged();
        }
        return ItemStack.EMPTY;
    }
}