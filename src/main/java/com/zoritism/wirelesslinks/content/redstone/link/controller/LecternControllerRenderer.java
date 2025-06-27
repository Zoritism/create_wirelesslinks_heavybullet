package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.foundation.item.PartialItemModelRenderer;
import com.zoritism.wirelesslinks.foundation.item.CustomRenderedItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/**
 * Рендерит Linked Controller на LecternControllerBlock.
 * Аналогично Create, использует custom renderer предмета.
 */
public class LecternControllerRenderer implements BlockEntityRenderer<LecternControllerBlockEntity> {

    public LecternControllerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LecternControllerBlockEntity be, float partialTicks, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        ItemStack controller = be.getController();
        if (controller.isEmpty())
            return;

        BlockState state = be.getBlockState();
        Direction facing = Direction.NORTH;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING))
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);

        ps.pushPose();

        // Смещение в центр + чуть выше, чтобы контроллер лежал на лекторне
        ps.translate(0.5, 1.02, 0.5);

        // Поворот по facing, чтобы контроллер был ориентирован правильно
        switch (facing) {
            case SOUTH -> ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
            case EAST -> ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            default -> { }
        }

        // Лежит на поверхности
        ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        ps.scale(1.07f, 1.07f, 1.07f);

        // Используем custom renderer из LinkedControllerItemRenderer
        CustomRenderedItemModel model = (CustomRenderedItemModel) Minecraft.getInstance()
                .getItemRenderer().getModel(controller, null, null, 0);

        PartialItemModelRenderer renderer = PartialItemModelRenderer.of(controller,
                ItemDisplayContext.FIXED, ps, buf, overlay);

        // Отрисовываем item как на лекторне
        LinkedControllerItemRenderer.renderInLectern(
                controller, model, renderer, ItemDisplayContext.FIXED, ps, light, true, false
        );

        ps.popPose();
    }
}