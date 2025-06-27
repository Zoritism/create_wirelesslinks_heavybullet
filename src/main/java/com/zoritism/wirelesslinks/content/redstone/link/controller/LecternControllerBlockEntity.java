package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.foundation.blockEntity.SmartBlockEntity;
import com.zoritism.wirelesslinks.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.zoritism.wirelesslinks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;
import java.util.UUID;

public class LecternControllerBlockEntity extends SmartBlockEntity {

    private CompoundTag controllerNbt = new CompoundTag();
    private UUID user;
    private UUID prevUser;
    private boolean deactivatedThisTick;

    public LecternControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // Нет особенностей
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("ControllerData", controllerNbt);
        if (user != null)
            tag.putUUID("User", user);
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        tag.put("ControllerData", controllerNbt);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        controllerNbt = tag.getCompound("ControllerData");
        user = tag.hasUUID("User") ? tag.getUUID("User") : null;
    }

    /**
     * Получить предмет контроллера, лежащий в лекторне. Полностью копирует NBT.
     */
    public ItemStack getController() {
        if (controllerNbt == null || controllerNbt.isEmpty())
            return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ModItems.LINKED_CONTROLLER.get());
        stack.setTag(controllerNbt.copy());
        return stack;
    }

    /**
     * Установить контроллер с NBT в лекторн.
     */
    public void setController(ItemStack newController) {
        if (newController != null && newController.getItem() == ModItems.LINKED_CONTROLLER.get()) {
            controllerNbt = newController.hasTag() ? newController.getTag().copy() : new CompoundTag();
            sendData();
        }
    }

    public boolean hasUser() {
        return user != null;
    }

    public boolean isUsedBy(Player player) {
        return hasUser() && user.equals(player.getUUID());
    }

    /**
     * Начать использование контроллера через лекторн (если никто не использует).
     */
    public void tryStartUsing(Player player) {
        if (!deactivatedThisTick && !hasUser() && playerInRange(player, level, worldPosition))
            startUsing(player);
    }

    /**
     * Прекратить использование контроллера через лекторн.
     */
    public void tryStopUsing(Player player) {
        if (isUsedBy(player))
            stopUsing(player);
    }

    private void startUsing(Player player) {
        user = player.getUUID();
        player.getPersistentData().putBoolean("IsUsingLecternController", true);
        sendData();
    }

    private void stopUsing(Player player) {
        user = null;
        if (player != null)
            player.getPersistentData().remove("IsUsingLecternController");
        deactivatedThisTick = true;
        sendData();
    }

    public static boolean playerIsUsingLectern(Player player) {
        return player.getPersistentData().contains("IsUsingLecternController");
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::tryToggleActive);
            prevUser = user;
        }

        if (!level.isClientSide) {
            deactivatedThisTick = false;

            if (!(level instanceof ServerLevel))
                return;
            if (user == null)
                return;

            Entity entity = ((ServerLevel) level).getEntity(user);
            if (!(entity instanceof Player player)) {
                stopUsing(null);
                return;
            }

            if (!playerInRange(player, level, worldPosition) || !playerIsUsingLectern(player))
                stopUsing(player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tryToggleActive() {
        Player clientPlayer = net.minecraft.client.Minecraft.getInstance().player;
        // Если пользователь закончился (был активен только что) — деактивируем на клиенте
        if (user == null && clientPlayer != null && clientPlayer.getUUID().equals(prevUser)) {
            LinkedControllerClientHandler.deactivateInLectern();
        }
        // Если только что появился пользователь — активируем на клиенте
        else if (user != null && (prevUser == null || !user.equals(prevUser)) && clientPlayer != null && clientPlayer.getUUID().equals(user)) {
            LinkedControllerClientHandler.activateInLectern(worldPosition);
        }
    }

    /**
     * Извлечь контроллер из лекторна и выбросить в мир (с сохранением NBT).
     */
    public void dropController(BlockState state) {
        if (controllerNbt == null || controllerNbt.isEmpty())
            return;

        Entity playerEntity = null;
        if (level instanceof ServerLevel serverLevel && user != null) {
            playerEntity = serverLevel.getEntity(user);
        }
        if (playerEntity instanceof Player)
            stopUsing((Player) playerEntity);

        // Исправлено: используем HORIZONTAL_FACING для корректного направления
        Direction dir = null;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            dir = state.getValue(BlockStateProperties.FACING);
        } else {
            dir = Direction.NORTH;
        }

        double x = worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, getController());
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        controllerNbt = new CompoundTag();
        sendData();
    }

    /**
     * Проверка дистанции игрока до лекторна (по ForgeMod.BLOCK_REACH).
     */
    public static boolean playerInRange(Player player, Level world, BlockPos pos) {
        double reach = 0.4 * player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        return player.distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
    }
}