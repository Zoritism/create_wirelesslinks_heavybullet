package com.zoritism.wirelesslinks.foundation.block.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Block;

public class CustomBlockModels {

	private final Map<Block, Function<BakedModel, ? extends BakedModel>> finalModelFuncs = new IdentityHashMap<>();

	public void register(Block block, Function<BakedModel, ? extends BakedModel> func) {
		finalModelFuncs.put(block, func);
	}

	public void forEach(BiConsumer<Block, Function<BakedModel, ? extends BakedModel>> consumer) {
		finalModelFuncs.forEach(consumer);
	}
}