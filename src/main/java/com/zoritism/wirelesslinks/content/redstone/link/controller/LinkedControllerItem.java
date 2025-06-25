package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlock;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.FrequencyPair;
import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Consumer;

public class LinkedControllerItem extends Item implements MenuProvider {

	public static final int SLOT_COUNT = 12 * 2;

	public LinkedControllerItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null)
			return InteractionResult.PASS;

		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);

		// Взаимодействие с Redstone Link для биндинга
		if (state.getBlock() instanceof RedstoneLinkBlock) {
			if (level.isClientSide) {
				LinkedControllerClientHandler.toggleBindMode(pos);
			} else {
				player.getCooldowns().addCooldown(this, 5);
			}
			return InteractionResult.SUCCESS;
		}

		// Вставка в лекторн
		if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
			if (!level.isClientSide) {
				ItemStack copy = player.isCreative() ? stack.copy() : stack.split(1);
				LecternBlock.tryPlaceBook(player, level, pos, state, copy);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		// --- КАК В CREATE --- //
		// Открытие меню частот по Ctrl+ПКМ (main hand)
		if (isCtrlDown(player) && hand == InteractionHand.MAIN_HAND) {
			if (!level.isClientSide && player instanceof ServerPlayer sp)
				sp.openMenu(this);
			return InteractionResultHolder.success(stack);
		}

		// Обычный ПКМ — включение режима управления (без Ctrl)
		if (!isCtrlDown(player)) {
			if (level.isClientSide)
				LinkedControllerClientHandler.toggle();
			player.getCooldowns().addCooldown(this, 5);
		}

		return InteractionResultHolder.pass(stack);
	}

	// Проверка нажатия Ctrl (как в Create)
	private static boolean isCtrlDown(Player player) {
		// На сервере или в headless режиме всегда false
		if (player == null || player.level().isClientSide == false)
			return false;
		// На клиенте — используем системную клавишу (minecraft mapping)
		// (В Create используется InputConstants и KeyMapping)
		return net.minecraft.client.Minecraft.getInstance().options.keyControl.isDown();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		ItemStack held = player.getMainHandItem();
		return LinkedControllerMenu.create(id, inv, held);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(getDescriptionId());
	}

	public static ItemStackHandler getFrequencyInventory(ItemStack controller) {
		ItemStackHandler inv = new ItemStackHandler(SLOT_COUNT);
		if (controller.getItem() != ModItems.LINKED_CONTROLLER.get())
			return inv;
		CompoundTag tag = controller.getOrCreateTagElement("Items");
		if (!tag.isEmpty())
			inv.deserializeNBT(tag);
		return inv;
	}

	public static void saveFrequencyInventory(ItemStack controller, ItemStackHandler inv) {
		controller.getOrCreateTag().put("Items", inv.serializeNBT());
	}

	public static FrequencyPair slotToFrequency(ItemStack controller, int logicalSlot) {
		ItemStackHandler inv = getFrequencyInventory(controller);
		int aIdx = logicalSlot * 2;
		int bIdx = aIdx + 1;
		ItemStack a = inv.getStackInSlot(aIdx);
		ItemStack b = inv.getStackInSlot(bIdx);
		return FrequencyPair.of(a, b);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final LinkedControllerItemRenderer renderer = new LinkedControllerItemRenderer();

			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}
}