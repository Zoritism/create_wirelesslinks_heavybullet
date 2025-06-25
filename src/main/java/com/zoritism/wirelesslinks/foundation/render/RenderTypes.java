package com.zoritism.wirelesslinks.foundation.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RenderTypes {

	private static final RenderStateShard.TransparencyStateShard CUSTOM_TRANSLUCENT_TRANSPARENCY =
			new RenderStateShard.TransparencyStateShard("custom_translucent", () -> {
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
			}, RenderSystem::disableBlend);

	private static final RenderType ITEM_GLOWING_SOLID = RenderType.create("item_glowing_solid",
			DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, true, false, RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(
							new ResourceLocation("textures/atlas/blocks.png"), false, false))
					.setLightmapState(new RenderStateShard.LightmapStateShard(true))
					.setOverlayState(new RenderStateShard.OverlayStateShard(true))
					.createCompositeState(true));

	private static final RenderType ITEM_GLOWING_TRANSLUCENT = RenderType.create("item_glowing_translucent",
			DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(
							new ResourceLocation("textures/atlas/blocks.png"), false, false))
					.setTransparencyState(CUSTOM_TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(new RenderStateShard.LightmapStateShard(true))
					.setOverlayState(new RenderStateShard.OverlayStateShard(true))
					.createCompositeState(true));

	public static RenderType itemGlowingSolid() {
		return ITEM_GLOWING_SOLID;
	}

	public static RenderType itemGlowingTranslucent() {
		return ITEM_GLOWING_TRANSLUCENT;
	}
}
