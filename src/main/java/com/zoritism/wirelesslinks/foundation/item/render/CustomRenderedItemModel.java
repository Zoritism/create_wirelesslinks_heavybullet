package com.simibubi.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

public class CustomRenderedItemModel extends BakedModelWrapper<BakedModel> {

	public CustomRenderedItemModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public boolean isCustomRenderer() {
		return true;
	}

	@Override
	public BakedModel applyTransform(ItemDisplayContext cameraItemDisplayContext, PoseStack mat, boolean leftHand) {
		// Возвращаем this вместо оригинальной модели — важно для кастомного рендера
		super.applyTransform(cameraItemDisplayContext, mat, leftHand);
		return this;
	}

	public BakedModel getOriginalModel() {
		return originalModel;
	}
}