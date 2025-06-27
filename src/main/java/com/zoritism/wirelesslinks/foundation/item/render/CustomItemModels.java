package com.zoritism.wirelesslinks.foundation.item.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomItemModels {

	private final Map<Item, Function<BakedModel, ? extends BakedModel>> finalModelFuncs = new IdentityHashMap<>();

	public void register(Item item, Function<BakedModel, ? extends BakedModel> func) {
		finalModelFuncs.put(item, func);
	}

	public void forEach(BiConsumer<Item, Function<BakedModel, ? extends BakedModel>> consumer) {
		finalModelFuncs.forEach(consumer);
	}
}