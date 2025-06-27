package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import com.zoritism.wirelesslinks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;

public class LecternControllerBlock extends Block implements EntityBlock {

    public LecternControllerBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.POWERED, false)
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.POWERED, BlockStateProperties.FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LecternControllerBlockEntity(ModBlockEntities.LECTERN_CONTROLLER.get(), pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LecternControllerBlockEntity lectern))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. Вставить контроллер в лекторн (если его нет)
        if (lectern.getController().isEmpty() && held.is(ModItems.LINKED_CONTROLLER.get())) {
            ItemStack insert = player.isCreative() ? held.copy() : held.split(1);
            lectern.setController(insert);
            lectern.sendData();
            return InteractionResult.SUCCESS;
        }

        // 2. CTRL+ПКМ (извлечь контроллер)
        if (player.isShiftKeyDown() && !lectern.getController().isEmpty()) {
            lectern.dropController(state);
            lectern.sendData();
            return InteractionResult.SUCCESS;
        }

        // 3. ПКМ — активировать управление (если контроллер уже есть)
        if (!lectern.getController().isEmpty()) {
            if (!level.isClientSide)
                lectern.tryStartUsing(player);
            // На клиенте сразу активируем режим управления (через ClientHandler)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LecternControllerBlockEntity beClient = (LecternControllerBlockEntity) level.getBlockEntity(pos);
                if (beClient != null)
                    beClient.tryStartUsing(player);
            });
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
            }
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        // Если потребуется реакция на сигнал/энергию — добавь тут
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    // Для корректного взаимодействия с ItemEntity при извлечении контроллера
    protected void spawnAsEntity(Level level, BlockPos pos, ItemStack stack, BlockState state) {
        Direction dir = state.hasProperty(BlockStateProperties.FACING)
                ? state.getValue(BlockStateProperties.FACING)
                : Direction.NORTH;
        double x = pos.getX() + 0.5 + 0.25 * dir.getStepX();
        double y = pos.getY() + 1;
        double z = pos.getZ() + 0.5 + 0.25 * dir.getStepZ();
        ItemEntity itementity = new ItemEntity(level, x, y, z, stack);
        itementity.setDefaultPickUpDelay();
        level.addFreshEntity(itementity);
    }

    @OnlyIn(Dist.CLIENT)

    public boolean shouldDisplayFluidOverlay(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.level.material.FluidState fluidState) {
        return false;
    }
}