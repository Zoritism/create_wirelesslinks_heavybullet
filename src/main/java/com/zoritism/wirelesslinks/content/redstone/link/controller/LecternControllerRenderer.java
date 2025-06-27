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
 * Рендерит Linked Controller на LecternControllerBlock аналогично Create.
 * Использует custom renderer предмета и позиционирует контроллер-книгу на лекторне.
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
        // Используем horizontal_facing для корректного поворота, как в Create
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING))
            facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

        ps.pushPose();

        // Позиционирование и наклон контроллера-книги на лекторне (по образцу Create)
        ps.translate(0.5, 1.45, 0.5);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(horizontalAngle(facing) - 90));
        ps.translate(0.28, 0, 0);
        ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-22.0f));

        // Используем custom renderer из LinkedControllerItemRenderer
        CustomRenderedItemModel model = (CustomRenderedItemModel) Minecraft.getInstance()
                .getItemRenderer().getModel(controller, be.getLevel(), null, 0);

        PartialItemModelRenderer renderer = PartialItemModelRenderer.of(controller,
                ItemDisplayContext.NONE, ps, buf, overlay);

        // Определяем активность и депрессию (нажатие) по LecternControllerBlockEntity
        boolean active = be.hasUser();
        boolean renderDepression = false;
        if (Minecraft.getInstance().player != null) {
            renderDepression = be.isUsedBy(Minecraft.getInstance().player);
        }

        LinkedControllerItemRenderer.renderInLectern(
                controller, model, renderer, ItemDisplayContext.NONE, ps, light, active, renderDepression
        );

        ps.popPose();
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