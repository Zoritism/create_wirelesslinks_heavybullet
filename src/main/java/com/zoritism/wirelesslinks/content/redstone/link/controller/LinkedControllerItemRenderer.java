package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zoritism.wirelesslinks.WirelessLinksMod;
import com.zoritism.wirelesslinks.content.redstone.link.controller.LinkedControllerClientHandler.Mode;
import com.zoritism.wirelesslinks.foundation.animation.AnimationTickHolder;
import com.zoritism.wirelesslinks.foundation.animation.LerpedFloat;
import com.zoritism.wirelesslinks.foundation.animation.LerpedFloat.Chaser;
import com.zoritism.wirelesslinks.foundation.item.CustomRenderedItemModel;
import com.zoritism.wirelesslinks.foundation.item.CustomRenderedItemModelRenderer;
import com.zoritism.wirelesslinks.foundation.item.PartialItemModelRenderer;
import com.zoritism.wirelesslinks.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerItemRenderer extends CustomRenderedItemModelRenderer {

	private static final ResourceLocation POWERED = new ResourceLocation(WirelessLinksMod.MODID, "item/linked_controller/powered");
	private static final ResourceLocation BUTTON = new ResourceLocation(WirelessLinksMod.MODID, "item/linked_controller/button");

	private static final LerpedFloat equipProgress = LerpedFloat.linear().startWithValue(0);
	private static final List<LerpedFloat> buttons = new ArrayList<>(6);

	static {
		for (int i = 0; i < 6; i++) {
			buttons.add(LerpedFloat.linear().startWithValue(0));
		}
	}

	public static void tick() {
		if (Minecraft.getInstance().isPaused())
			return;

		boolean active = LinkedControllerClientHandler.MODE != Mode.IDLE;
		equipProgress.chase(active ? 1 : 0, .2f, Chaser.EXP);
		equipProgress.tickChaser();

		if (!active)
			return;

		for (int i = 0; i < buttons.size(); i++) {
			LerpedFloat f = buttons.get(i);
			f.chase(LinkedControllerClientHandler.currentlyPressed.contains(i) ? 1 : 0, .4f, Chaser.EXP);
			f.tickChaser();
		}
	}

	public static void resetButtons() {
		buttons.forEach(b -> b.startWithValue(0));
	}

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel original, PartialItemModelRenderer pir, ItemDisplayContext ctx, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
		renderInternal(stack, original, pir, ctx, ms, light, RenderType.NORMAL, false);
	}

	public static void renderInLectern(ItemStack stack, CustomRenderedItemModel original, PartialItemModelRenderer pir, ItemDisplayContext ctx, PoseStack ms, int light, boolean active, boolean depress) {
		renderInternal(stack, original, pir, ctx, ms, light, RenderType.LECTERN, active);
	}

	private static void renderInternal(ItemStack stack, CustomRenderedItemModel original, PartialItemModelRenderer pir, ItemDisplayContext ctx, PoseStack ms, int light, RenderType type, boolean forceActive) {
		float pt = AnimationTickHolder.getPartialTicks();
		ms.pushPose();

		boolean active = forceActive;
		if (type == RenderType.NORMAL) {
			Minecraft mc = Minecraft.getInstance();
			boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
			ItemDisplayContext main = rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			ItemDisplayContext off = rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

			boolean noInMain = !mc.player.getMainHandItem().is(ModItems.LINKED_CONTROLLER.get());
			if (ctx == main || (ctx == off && noInMain)) {
				float eq = equipProgress.getValue(pt);
				int m = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
				ms.translate(0, eq / 4, eq / 4 * m);
				ms.mulPose(Axis.YP.rotationDegrees(eq * -30 * m));
				ms.mulPose(Axis.ZP.rotationDegrees(eq * -30));
				active = true;
			}
			if (ctx == ItemDisplayContext.GUI) {
				if (stack == mc.player.getMainHandItem() || (stack == mc.player.getOffhandItem() && noInMain))
					active = true;
			}
			active &= LinkedControllerClientHandler.MODE != Mode.IDLE;
		}

		BakedModel base = active
				? Minecraft.getInstance().getModelManager().getModel(POWERED)
				: original.getOriginalModel();
		pir.render(base, light);

		if (!active) {
			ms.popPose();
			return;
		}

		BakedModel button = Minecraft.getInstance().getModelManager().getModel(BUTTON);
		float s = 1 / 16f, depress = s * -.75f;

		if (type == RenderType.NORMAL && LinkedControllerClientHandler.MODE == Mode.BIND) {
			int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 2, 5, 15);
			light = i << 20;
		}

		renderButtonGrid(pir, ms, light, pt, button, depress);
		ms.popPose();
	}

	private static void renderButtonGrid(PartialItemModelRenderer pir, PoseStack ms, int light, float pt, BakedModel button, float depress) {
		float s = 1 / 16f;
		int idx = 0;

		ms.pushPose();
		ms.translate(2 * s, 0, 8 * s);
		renderButton(pir, ms, light, pt, button, depress, idx++);
		ms.translate(4 * s, 0, 0);
		renderButton(pir, ms, light, pt, button, depress, idx++);
		ms.translate(-2 * s, 0, 2 * s);
		renderButton(pir, ms, light, pt, button, depress, idx++);
		ms.translate(0, 0, -4 * s);
		renderButton(pir, ms, light, pt, button, depress, idx++);
		ms.popPose();

		ms.translate(3 * s, 0, 3 * s);
		renderButton(pir, ms, light, pt, button, depress, idx++);
		ms.translate(2 * s, 0, 0);
		renderButton(pir, ms, light, pt, button, depress, idx);
	}

	private static void renderButton(PartialItemModelRenderer pir, PoseStack ms, int light, float pt, BakedModel button, float depress, int idx) {
		ms.pushPose();
		float d = depress * buttons.get(idx).getValue(pt);
		ms.translate(0, d, 0);
		pir.renderSolid(button, light);
		ms.popPose();
	}

	private enum RenderType {
		NORMAL, LECTERN
	}
}
