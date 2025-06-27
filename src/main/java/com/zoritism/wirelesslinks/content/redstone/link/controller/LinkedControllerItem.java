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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class LinkedControllerItem extends Item implements MenuProvider {
	public static final int SLOT_COUNT = 12 * 2;

	public LinkedControllerItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null) return InteractionResult.PASS;
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);

		// Делегация логики блоку лекторна — только инициируем действия!
		if (state.getBlock() == ModBlocks.LECTERN_CONTROLLER.get()) {
			// Shift+ПКМ по своему лекторну — инициируем извлечение через блок
			if (player.isShiftKeyDown()) {
				if (!level.isClientSide) {
					var be = level.getBlockEntity(pos);
					if (be instanceof LecternControllerBlockEntity lectern) {
						ItemStack lecternController = lectern.getController();
						if (!lecternController.isEmpty()) {
							lectern.dropController(state);
							lectern.sendData();
							return InteractionResult.SUCCESS;
						}
					}
				}
				return InteractionResult.SUCCESS;
			}
			// Обычный ПКМ по своему лекторну — передаём управление блоку (возвращаем PASS)
			return InteractionResult.PASS;
		}

		// ПКМ по ванильному лекторну (без книги) — заменяем на свой лекторн через блок
		if (state.is(Blocks.LECTERN) && !state.getValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK)) {
			if (!level.isClientSide) {
				ItemStack stack = player.isCreative() ? ctx.getItemInHand().copy() : ctx.getItemInHand().split(1);
				// Меняем на свой лекторн
				BlockState newState = ModBlocks.LECTERN_CONTROLLER.get().defaultBlockState()
						.setValue(net.minecraft.world.level.block.LecternBlock.FACING, state.getValue(net.minecraft.world.level.block.LecternBlock.FACING))
						.setValue(net.minecraft.world.level.block.LecternBlock.POWERED, state.getValue(net.minecraft.world.level.block.LecternBlock.POWERED));
				level.setBlockAndUpdate(pos, newState);
				var be = level.getBlockEntity(pos);
				if (be instanceof LecternControllerBlockEntity lectern) {
					lectern.setController(stack);
					lectern.sendData();
				}
			}
			return InteractionResult.SUCCESS;
		}

		// Поведение для других блоков (редстоун-линк и т.п.)
		if (player.mayBuild() && !player.isShiftKeyDown()) {
			if (state.getBlock() instanceof RedstoneLinkBlock) {
				if (level.isClientSide)
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LinkedControllerClientHandler.toggleBindMode(pos));
				player.getCooldowns().addCooldown(this, 2);
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	@Deprecated
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		return InteractionResult.PASS;
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

		// НЕ переходить в режим управления если целимся в лекторн (это уже обработает useOn!)
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
}