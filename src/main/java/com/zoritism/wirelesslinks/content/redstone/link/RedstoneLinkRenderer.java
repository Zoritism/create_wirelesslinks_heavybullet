package com.zoritism.wirelesslinks.content.redstone.link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RedstoneLinkRenderer implements BlockEntityRenderer<RedstoneLinkBlockEntity> {

    public RedstoneLinkRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RedstoneLinkBlockEntity be, float partialTicks, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (be.getBlockPos().distSqr(player.blockPosition()) > 64.0) return;

        boolean showCount = player.isShiftKeyDown();
        Font font = mc.font;

        ItemStack left = be.getLeftFrequencyStack();
        ItemStack right = be.getRightFrequencyStack();

        Direction facing = be.getBlockState().getValue(RedstoneLinkBlock.FACING);
        boolean vertical = (facing == Direction.UP || facing == Direction.DOWN);

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        if (!vertical)
            ps.mulPose(facing.getOpposite().getRotation()); // "вперёд" — наружу блока

        if (facing == Direction.DOWN) {
            // === ПРАВАЯ миниатюра (верхняя визуально) ===
            if (!right.isEmpty()) {
                ps.pushPose();
                ps.translate(0.0, 0.2, -0.15);
                ps.scale(0.35f, 0.35f, 0.35f);
                mc.getItemRenderer().renderStatic(right, ItemDisplayContext.FIXED, light, overlay, ps, buf, null, 0);
                ps.popPose();

                if (showCount) {
                    ps.pushPose();
                    ps.translate(0.0, 0.15, -0.15);
                    rotateTowardPlayer(ps, be, player);
                    drawCount(font, buf, ps, light, right.getCount());
                    ps.popPose();
                }
            }

            // === ЛЕВАЯ миниатюра (нижняя визуально) ===
            if (!left.isEmpty()) {
                ps.pushPose();
                ps.translate(0.0, 0.2, 0.15);
                ps.scale(0.35f, 0.35f, 0.35f);
                mc.getItemRenderer().renderStatic(left, ItemDisplayContext.FIXED, light, overlay, ps, buf, null, 0);
                ps.popPose();

                if (showCount) {
                    ps.pushPose();
                    ps.translate(0.0, 0.15, 0.15);
                    rotateTowardPlayer(ps, be, player);
                    drawCount(font, buf, ps, light, left.getCount());
                    ps.popPose();
                }
            }

        } else {
            // === ЛЕВАЯ миниатюра (верхняя) ===
            if (!left.isEmpty()) {
                ps.pushPose();
                if (vertical) {
                    ps.translate(0.0, -0.2, -0.15);
                } else {
                    ps.translate(0.0, 0.20, 0.19);
                }
                ps.scale(0.35f, 0.35f, 0.35f);
                mc.getItemRenderer().renderStatic(left, ItemDisplayContext.FIXED, light, overlay, ps, buf, null, 0);
                ps.popPose();

                if (showCount) {
                    ps.pushPose();
                    if (vertical) {
                        ps.translate(0.0, -0.02, -0.15);
                    } else {
                        ps.translate(0.0, 0.2, 0.0);
                    }
                    rotateTowardPlayer(ps, be, player);
                    drawCount(font, buf, ps, light, left.getCount());
                    ps.popPose();
                }
            }

            // === ПРАВАЯ миниатюра (нижняя) ===
            if (!right.isEmpty()) {
                ps.pushPose();
                if (vertical) {
                    ps.translate(0.0, -0.2, 0.15);
                } else {
                    ps.translate(0.0, 0.20, -0.19);
                }
                ps.scale(0.35f, 0.35f, 0.35f);
                mc.getItemRenderer().renderStatic(right, ItemDisplayContext.FIXED, light, overlay, ps, buf, null, 0);
                ps.popPose();

                if (showCount) {
                    ps.pushPose();
                    if (vertical) {
                        ps.translate(0.0, -0.02, 0.15);
                    } else {
                        ps.translate(0.0, 0.2, -0.4);
                    }
                    rotateTowardPlayer(ps, be, player);
                    drawCount(font, buf, ps, light, right.getCount());
                    ps.popPose();
                }
            }
        }

        ps.popPose();
    }

    private static void drawCount(Font font, MultiBufferSource buf, PoseStack ps, int light, int count) {
        ps.scale(0.01f, -0.01f, 0.01f);
        String txt = String.valueOf(count);
        float xOff = -font.width(txt) / 2f;
        font.drawInBatch(txt, xOff, 0, 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
    }

    private static void rotateTowardPlayer(PoseStack ps, RedstoneLinkBlockEntity be, Player player) {
        Direction facing = be.getBlockState().getValue(RedstoneLinkBlock.FACING);

        // Укладываем текст горизонтально на плоскость блока
        switch (facing) {
            case UP -> {
                // ничего
            }
            case DOWN -> {
                ps.mulPose(Axis.XP.rotationDegrees(180));
            }
            case NORTH, SOUTH, EAST, WEST -> {
                ps.mulPose(Axis.XP.rotationDegrees(90));
            }
        }

        // Поворот текста к игроку
        double dx = player.getX() - (be.getBlockPos().getX() + 0.5);
        double dz = player.getZ() - (be.getBlockPos().getZ() + 0.5);
        float yaw = (float) (Mth.atan2(dz, dx) * (180f / Math.PI)) - 90f;

        if (facing == Direction.UP) {
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        } else if (facing == Direction.DOWN) {
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            switch (facing) {
                case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
                case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            }
            ps.mulPose(Axis.XP.rotationDegrees(180));
        }
    }
}
