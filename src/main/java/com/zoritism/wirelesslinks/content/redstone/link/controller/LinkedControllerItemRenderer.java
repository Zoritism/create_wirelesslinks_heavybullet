package com.zoritism.wirelesslinks.content.redstone.link.controller;

import java.util.Vector;

import com.mojang.blaze3d.vertex.PoseStack;
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

	protected static final ResourceLocation POWERED = new ResourceLocation(WirelessLinksMod.MODID, "item/linked_controller/powered");
	protected static final ResourceLocation BASE = new ResourceLocation(WirelessLinksMod.MODID, "item/linked_controller/item");
	protected static final ResourceLocation BUTTON = new ResourceLocation(WirelessLinksMod.MODID, "item/linked_controller/button");

	static LerpedFloat equipProgress;
	static Vector<LerpedFloat> buttons;

	static {
		equipProgress = LerpedFloat.linear().startWithValue(0);
		buttons = new Vector<>(6);
		for (int i = 0; i < 6; i++)
			buttons.add(LerpedFloat.linear().startWithValue(0));
	}

	public static void ensureClientHandlerLoaded() {}

	public static void tick() {
		if (Minecraft.getInstance().isPaused())
			return;

		boolean active = LinkedControllerClientHandler.MODE != Mode.IDLE;
		equipProgress.chase(active ? 1 : 0, .2f, Chaser.EXP);
		equipProgress.tickChaser();

		for (int i = 0; i < buttons.size(); i++) {
			LerpedFloat lerpedFloat = buttons.get(i);
			lerpedFloat.chase(LinkedControllerClientHandler.currentlyPressed.contains(i) ? 1 : 0, .4f, Chaser.EXP);
			lerpedFloat.tickChaser();
		}
	}

	public static void resetButtons() {
		for (LerpedFloat button : buttons) {
			button.startWithValue(0);
		}
	}

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
						  ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light,
						  int overlay) {
		renderNormal(stack, model, renderer, transformType, ms, light);
	}

	protected static void renderNormal(ItemStack stack, CustomRenderedItemModel model,
									   PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
									   int light) {
		render(stack, model, renderer, transformType, ms, light, RenderType.NORMAL, false, false);
	}

	public static void renderInLectern(ItemStack stack, CustomRenderedItemModel model,
									   PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
									   int light, boolean active, boolean renderDepression) {
		render(stack, model, renderer, transformType, ms, light, RenderType.LECTERN, active, renderDepression);
	}

	protected static void render(ItemStack stack, CustomRenderedItemModel model,
								 PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
								 int light, RenderType renderType, boolean active, boolean renderDepression) {
		float pt = AnimationTickHolder.getPartialTicks();

		ms.pushPose();

		boolean powered = false;

		if (renderType == RenderType.NORMAL) {
			Minecraft mc = Minecraft.getInstance();
			boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
			ItemDisplayContext mainHand =
					rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			ItemDisplayContext offHand =
					rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

			active = false;
			boolean noControllerInMain = !mc.player.getMainHandItem().is(ModItems.LINKED_CONTROLLER.get());

			if (transformType == mainHand || (transformType == offHand && noControllerInMain)) {
				float equip = equipProgress.getValue(pt);
				int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
				ms.translate(0, equip / 4, equip / 4 * handModifier);
				ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(equip * -30 * handModifier));
				ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(equip * -30));
				active = true;
			}

			if (transformType == ItemDisplayContext.GUI) {
				if (stack == mc.player.getMainHandItem())
					active = true;
				if (stack == mc.player.getOffhandItem() && noControllerInMain)
					active = true;
			}

			// powered теперь определяется только по NBT
			powered = stack.hasTag() && stack.getTag().getBoolean("Powered");
			renderDepression = true;
		}

		BakedModel base = Minecraft.getInstance().getModelManager().getModel(
				powered ? POWERED : BASE
		);
		renderer.render(base, light);

		if (!active && !powered) {
			ms.popPose();
			return;
		}

		BakedModel button = Minecraft.getInstance().getModelManager().getModel(BUTTON);
		float s = 1 / 16f;
		float b = s * -.75f;
		int index = 0;

		if (renderType == RenderType.NORMAL) {
			if (LinkedControllerClientHandler.MODE == Mode.BIND) {
				int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4f) + 1) / 2, 5, 15);
				light = i << 20;
			}
		}

		ms.pushPose();
		ms.translate(2 * s, 0, 8 * s);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
		ms.translate(4 * s, 0, 0);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
		ms.translate(-2 * s, 0, 2 * s);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
		ms.translate(0, 0, -4 * s);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
		ms.popPose();

		ms.translate(3 * s, 0, 3 * s);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);
		ms.translate(2 * s, 0, 0);
		renderButton(renderer, ms, light, pt, button, b, index++, renderDepression);

		ms.popPose();
	}

	protected static void renderButton(PartialItemModelRenderer renderer, PoseStack ms, int light, float pt, BakedModel button,
									   float b, int index, boolean renderDepression) {
		ms.pushPose();
		if (renderDepression) {
			float depression = b * buttons.get(index).getValue(pt);
			ms.translate(0, depression, 0);
		}
		renderer.renderSolid(button, light);
		ms.popPose();
	}

	protected enum RenderType {
		NORMAL, LECTERN;
	}
}