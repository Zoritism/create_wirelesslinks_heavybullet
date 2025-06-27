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

        // Положение контроллера на лекторне — смещение и поворот
        ps.pushPose();
        ps.translate(0.5, 1.05, 0.5); // чуть выше центра блока
        ps.scale(1.1f, 1.1f, 1.1f);
        ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));

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