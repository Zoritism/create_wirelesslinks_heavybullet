package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import com.zoritism.wirelesslinks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import javax.annotation.Nullable;

public class LecternControllerBlock extends LecternBlock {

    public LecternControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(HAS_BOOK, true)
                .setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LECTERN_CONTROLLER.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LecternControllerBlockEntity lectern))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        ItemStack lecternController = lectern.getController();

        // Вставить контроллер — теперь просто ПКМ (без ctrl/shift)
        if (lecternController.isEmpty() && held.getItem() == ModItems.LINKED_CONTROLLER.get()) {
            if (!level.isClientSide) {
                ItemStack insert = player.isCreative() ? held.copy() : held.split(1);
                lectern.setController(insert);
                lectern.sendData();
            }
            return InteractionResult.SUCCESS;
        }

        // SHIFT+ПКМ — извлечь контроллер и заменить на обычный lectern, при этом очищая NBT лекторна
        if (player.isShiftKeyDown() && !lecternController.isEmpty()) {
            if (!level.isClientSide) {
                lectern.dropController(state);
                // Очищаем NBT у лекторна после извлечения контроллера
                lectern.setController(ItemStack.EMPTY);
                lectern.sendData();
                replaceWithLectern(state, level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        // ПКМ — активировать управление
        if (!lecternController.isEmpty()) {
            if (!level.isClientSide)
                lectern.tryStartUsing(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LecternControllerBlockEntity lectern && !lectern.getController().isEmpty()) {
                lectern.dropController(oldState);
                // Очищаем NBT у лекторна после извлечения контроллера при удалении блока
                lectern.setController(ItemStack.EMPTY);
            }
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return 15;
    }

    public void replaceLectern(BlockState lecternState, Level world, BlockPos pos, ItemStack controller) {
        world.setBlockAndUpdate(pos, defaultBlockState()
                .setValue(FACING, lecternState.getValue(FACING))
                .setValue(HAS_BOOK, true)
                .setValue(POWERED, lecternState.getValue(POWERED)));
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof LecternControllerBlockEntity lectern)
            lectern.setController(controller);
    }

    public void replaceWithLectern(BlockState state, Level world, BlockPos pos) {
        world.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(HAS_BOOK, state.getValue(HAS_BOOK))
                .setValue(POWERED, state.getValue(POWERED)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        return Blocks.LECTERN.getCloneItemStack(state, target, world, pos, player);
    }
}