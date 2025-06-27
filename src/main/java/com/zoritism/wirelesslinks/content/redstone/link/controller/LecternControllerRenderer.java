package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoritism.wirelesslinks.foundation.item.PartialItemModelRenderer;
import com.zoritism.wirelesslinks.foundation.item.CustomRenderedItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

/**
 * Рендерит Linked Controller на LecternControllerBlock аналогично Create.
 * Использует custom renderer предмета и позиционирует контроллер-книгу на лекторне.
 */
public class LecternControllerRenderer implements BlockEntityRenderer<LecternControllerBlockEntity> {

    public LecternControllerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LecternControllerBlockEntity be, float partialTicks, PoseStack ms,
                       MultiBufferSource buffer, int light, int overlay) {

        // ВСЕГДА рендерим предмет-контроллер (как в Create)
        ItemStack stack = be.getController();
        if (stack.isEmpty())
            return;

        ItemDisplayContext transformType = ItemDisplayContext.NONE;
        CustomRenderedItemModel mainModel = (CustomRenderedItemModel) Minecraft.getInstance()
                .getItemRenderer()
                .getModel(stack, be.getLevel(), null, 0);
        PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, transformType, ms, buffer, overlay);
        boolean active = be.hasUser();
        boolean renderDepression = Minecraft.getInstance().player != null && be.isUsedBy(Minecraft.getInstance().player);

        Direction facing = Direction.NORTH;
        if (be.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            facing = be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }

        ms.pushPose();
        ms.translate(0.5, 1.45, 0.5);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(horizontalAngle(facing) - 90));
        ms.translate(0.28, 0, 0);
        ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-22.0f));

        LinkedControllerItemRenderer.renderInLectern(stack, mainModel, renderer, transformType, ms, light, active, renderDepression);

        ms.popPose();
    }

    // Вспомогательный метод для угла поворота по горизонтали (аналог AngleHelper.horizontalAngle)
    private static float horizontalAngle(Direction facing) {
        return switch (facing) {
            case NORTH -> 180f;
            case EAST -> -90f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            default -> 0f;
        };
    }
}