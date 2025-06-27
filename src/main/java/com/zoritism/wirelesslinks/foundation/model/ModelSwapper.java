package com.zoritism.wirelesslinks.foundation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.zoritism.wirelesslinks.foundation.item.CustomRenderedItemModel;
import com.zoritism.wirelesslinks.foundation.item.render.CustomRenderedItems;
import com.zoritism.wirelesslinks.foundation.item.render.CustomItemModels;
import com.zoritism.wirelesslinks.foundation.block.render.CustomBlockModels;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Аналог Create ModelSwapper для Wireless Links.
 * Подменяет модели блоков и предметов на этапе бейка.
 */
public class ModelSwapper {

	protected CustomBlockModels customBlockModels = new CustomBlockModels();
	protected CustomItemModels customItemModels = new CustomItemModels();

	public CustomBlockModels getCustomBlockModels() {
		return customBlockModels;
	}

	public CustomItemModels getCustomItemModels() {
		return customItemModels;
	}

	public void onModelBake(ModelEvent.ModifyBakingResult event) {
		Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();

		// Меняем модели блоков, если зарегистрированы
		customBlockModels.forEach((block, modelFunc) -> swapModels(modelRegistry, getAllBlockStateModelLocations(block), modelFunc));

		// Меняем модели предметов, если зарегистрированы
		customItemModels.forEach((item, modelFunc) -> swapModels(modelRegistry, getItemModelLocation(item), modelFunc));

		// Для предметов с кастомным рендером — подменяем на CustomRenderedItemModel
		CustomRenderedItems.forEach(item ->
				swapModels(modelRegistry, getItemModelLocation(item), CustomRenderedItemModel::new)
		);
	}

	public void registerListeners(IEventBus modEventBus) {
		modEventBus.addListener(this::onModelBake);
	}

	public static <T extends BakedModel> void swapModels(Map<ResourceLocation, BakedModel> modelRegistry,
														 List<ModelResourceLocation> locations, Function<BakedModel, T> factory) {
		for (ModelResourceLocation location : locations) {
			swapModels(modelRegistry, location, factory);
		}
	}

	public static <T extends BakedModel> void swapModels(Map<ResourceLocation, BakedModel> modelRegistry,
														 ModelResourceLocation location, Function<BakedModel, T> factory) {
		BakedModel original = modelRegistry.get(location);
		if (original != null) {
			modelRegistry.put(location, factory.apply(original));
		}
	}

	public static List<ModelResourceLocation> getAllBlockStateModelLocations(Block block) {
		List<ModelResourceLocation> models = new ArrayList<>();
		ResourceLocation blockRl = block.getRegistryName(); // Используем getRegistryName для Forge 1.20.x
		block.getStateDefinition().getPossibleStates().forEach(state -> {
			models.add(BlockModelShaper.stateToModelLocation(blockRl, state));
		});
		return models;
	}

	public static ModelResourceLocation getItemModelLocation(Item item) {
		ResourceLocation itemRl = item.getRegistryName(); // Аналогично
		return new ModelResourceLocation(itemRl, "inventory");
	}
}