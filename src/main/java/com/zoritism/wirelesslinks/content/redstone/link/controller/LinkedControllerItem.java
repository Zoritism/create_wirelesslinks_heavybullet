package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlock;
import com.zoritism.wirelesslinks.registry.ModItems;
import com.zoritism.wirelesslinks.registry.ModBlocks;
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
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class LinkedControllerItem extends Item implements MenuProvider {

	public static final int SLOT_COUNT = 12 * 2;

	public LinkedControllerItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null) return InteractionResult.PASS;
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);

		// 1. Если клик по нашему LecternControllerBlock
		if (state.getBlock() == ModBlocks.LECTERN_CONTROLLER.get()) {
			if (!level.isClientSide) {
				var be = level.getBlockEntity(pos);
				if (be instanceof LecternControllerBlockEntity lectern) {
					ItemStack lecternController = lectern.getController();
					// a. Если лекторн пустой — вставляем контроллер
					if (lecternController.isEmpty()) {
						ItemStack insert = player.isCreative() ? stack.copy() : stack.split(1);
						lectern.setController(insert);
						lectern.sendData();
						return InteractionResult.SUCCESS;
					}
					// b. Если лекторн уже содержит контроллер и игрок рядом — переходим в режим управления
					if (!lecternController.isEmpty() && isPlayerInLecternRange(player, pos)) {
						lectern.tryStartUsing(player);
						return InteractionResult.SUCCESS;
					}
				}
			}
			// На клиенте не обрабатываем (всё делается через сервер)
			return InteractionResult.SUCCESS;
		}

		// 2. Если клик по обычному редстоун линк — прежнее поведение
		if (player.mayBuild()) {
			if (player.isShiftKeyDown()) {
				// Здесь мог бы быть swapControllers для лекторна, если реализовано
			} else {
				if (state.getBlock() instanceof RedstoneLinkBlock) {
					if (level.isClientSide)
						DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LinkedControllerClientHandler.toggleBindMode(pos));
					player.getCooldowns().addCooldown(this, 2);
					return InteractionResult.SUCCESS;
				}

				if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
					if (!level.isClientSide) {
						ItemStack copy = player.isCreative() ? stack.copy() : stack.split(1);
						LecternBlock.tryPlaceBook(player, level, pos, state, copy);
					}
					return InteractionResult.SUCCESS;
				}
			}
		}

		// 3. Всё остальное — стандартное поведение (не переходить в режим управления!)
		return this.use(level, player, ctx.getHand()).getResult();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!level.isClientSide && player instanceof ServerPlayer sp) {
				NetworkHooks.openScreen(sp, this, buf -> buf.writeItem(stack));
			}
			return InteractionResultHolder.success(stack);
		}

		// --- ВНИМАНИЕ! ---
		// Теперь переход в режим управления происходит только если НЕ целимся в лекторн!
		// (Собственно, тут это уже не обработается, т.к. если клик был по лекторну — обработка выше.)
		if (!player.isShiftKeyDown()) {
			if (level.isClientSide)
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LinkedControllerClientHandler::toggle);
			player.getCooldowns().addCooldown(this, 2);
		}

		return InteractionResultHolder.pass(stack);
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
		if (!tag.isEmpty()) inv.deserializeNBT(tag);
		return inv;
	}

	public static void saveFrequencyInventory(ItemStack controller, ItemStackHandler inv) {
		controller.getOrCreateTag().put("Items", inv.serializeNBT());
	}

	/**
	 * Возвращает Couple<ItemStack> для передачи в сетевые обработчики (по Create).
	 */
	public static Couple<ItemStack> toFrequency(ItemStack controller, int logicalSlot) {
		ItemStackHandler inv = getFrequencyInventory(controller);
		int aIdx = logicalSlot * 2;
		int bIdx = aIdx + 1;
		ItemStack a = inv.getStackInSlot(aIdx);
		ItemStack b = inv.getStackInSlot(bIdx);
		return Couple.of(a, b);
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

	/** Проверка, находится ли игрок в квадрате 3x3 вокруг центра блока (радиус 1) */
	private static boolean isPlayerInLecternRange(Player player, BlockPos lecternPos) {
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();
		double cx = lecternPos.getX() + 0.5;
		double cy = lecternPos.getY() + 0.5;
		double cz = lecternPos.getZ() + 0.5;
		return Math.abs(px - cx) <= 1.0 && Math.abs(py - cy) <= 1.0 && Math.abs(pz - cz) <= 1.0;
	}
}