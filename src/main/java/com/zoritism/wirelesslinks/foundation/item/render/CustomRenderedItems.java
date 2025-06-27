package com.zoritism.wirelesslinks.foundation.item.render;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomRenderedItems {

	private static final Set<Item> ITEMS = new ReferenceOpenHashSet<>();
	private static boolean itemsFiltered = false;

	public static void register(Item item) {
		ITEMS.add(item);
	}

	public static void forEach(Consumer<Item> consumer) {
		if (!itemsFiltered) {
			Iterator<Item> iterator = ITEMS.iterator();
			while (iterator.hasNext()) {
				Item item = iterator.next();
				if (!ForgeRegistries.ITEMS.containsValue(item) || !(IClientItemExtensions.of(item)
						.getCustomRenderer() != null)) {
					iterator.remove();
				}
			}
			itemsFiltered = true;
		}
		ITEMS.forEach(consumer);
	}
}