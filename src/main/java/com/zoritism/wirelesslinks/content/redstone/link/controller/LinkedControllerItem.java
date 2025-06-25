package com.zoritism.wirelesslinks.content.redstone.link.controller;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkBlock;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency;
import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
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

		if (state.getBlock() instanceof RedstoneLinkBlock) {
			if (level.isClientSide) {
				LinkedControllerClientHandler.toggleBindMode(pos);
			} else {
				player.getCooldowns().addCooldown(this, 5);
			}
			return InteractionResult.SUCCESS;
		}

		if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
			if (!level.isClientSide) {
				ItemStack copy = player.isCreative() ? stack.copy() : stack.split(1);
				LecternBlock.tryPlaceBook(player, level, pos, state, copy);
			}
			return InteractionResult.SUCCESS;
		}

		// Ключевой момент: если не по спецблоку, пробуем обычное использование (ПКМ по воздуху)
		return this.use(level, player, ctx.getHand()).getResult();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!level.isClientSide && player instanceof ServerPlayer sp) {
				net.minecraftforge.network.NetworkHooks.openScreen(sp, this, buf -> buf.writeItem(stack));
			}
			return InteractionResultHolder.success(stack);
		}

		if (!player.isShiftKeyDown()) {
			if (level.isClientSide)
				net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
						net.minecraftforge.api.distmarker.Dist.CLIENT,
						() -> () -> LinkedControllerClientHandler.toggle()
				);
			player.getCooldowns().addCooldown(this, 2); // 2, как в Create
		}

		return InteractionResultHolder.pass(stack); // ВАЖНО: именно pass
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
	 * Возвращает частотную пару для данного логического слота (0..11) контроллера.
	 * ВАЖНО: Возвращает оригинальные ItemStack с их количеством и NBT — поведение полностью как у RedstoneLink.
	 */
	public static FrequencyPair slotToFrequency(ItemStack controller, int logicalSlot) {
		ItemStackHandler inv = getFrequencyInventory(controller);
		int aIdx = logicalSlot * 2;
		int bIdx = aIdx + 1;
		ItemStack a = inv.getStackInSlot(aIdx);
		ItemStack b = inv.getStackInSlot(bIdx);
		// --- ВАЖНО: возвращаем ItemStack с их count и NBT! ---
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

	/**
	 * Передаёт сигнал по частоте, указанной в слоте контроллера.
	 * @param level уровень
	 * @param playerId UUID игрока
	 * @param slotLogical логический слот (0..11)
	 * @param pressed состояние нажатия (true/false)
	 * @param heldPos позиция (обычно player.blockPosition() или lectern)
	 */
	public static void transmitPressedKeys(Level level, UUID playerId, int slotLogical, boolean pressed, BlockPos heldPos) {
		// ВАЖНО: тут должен использоваться настоящий контроллер игрока
		// (этот метод как пример, в реальной логике используйте контроллер из инвентаря игрока)
		ItemStack controller = new ItemStack(ModItems.LINKED_CONTROLLER.get());
		FrequencyPair pair = slotToFrequency(controller, slotLogical);

		if (pair.getFirst().getStack().isEmpty() && pair.getSecond().getStack().isEmpty())
			return;

		Couple<Frequency> couple = Couple.of(pair.getFirst(), pair.getSecond());

		LinkedControllerServerHandler.receivePressed(
				level, heldPos, playerId,
				List.of(couple), pressed
		);
	}
}