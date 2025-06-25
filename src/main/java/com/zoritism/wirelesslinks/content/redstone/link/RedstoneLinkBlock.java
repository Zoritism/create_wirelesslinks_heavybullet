package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.item.BlockItem;


import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class RedstoneLinkBlock extends Block implements EntityBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final DirectionProperty HORIZONTAL_FACING = DirectionProperty.create("horizontal_facing", Direction.Plane.HORIZONTAL);

	private static final VoxelShape BASE_SHAPE = Block.box(2, 2, 14, 14, 14, 16);
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			Direction horizontal = Direction.NORTH;
			SHAPES.put(facing, rotateShape(BASE_SHAPE, facing, horizontal));
		}
	}

	public RedstoneLinkBlock() {
		super(Properties.of().mapColor(MapColor.STONE).strength(1.5f).noOcclusion());
		registerDefaultState(defaultBlockState()
				.setValue(POWERED, false)
				.setValue(RECEIVER, false)
				.setValue(FACING, Direction.NORTH)
				.setValue(HORIZONTAL_FACING, Direction.NORTH));
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity link) {
			if (link.isListening())
				link.updateNetworkState(); // для приёмника
			else
				link.transmitFromNeighborChange(); // для передатчика
		}
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED, RECEIVER, FACING, HORIZONTAL_FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction face = ctx.getClickedFace();
		BlockPos support = ctx.getClickedPos().relative(face.getOpposite());

		if (!ctx.getLevel().getBlockState(support).isFaceSturdy(ctx.getLevel(), support, face))
			return null;

		BlockState state = defaultBlockState()
				.setValue(FACING, face)
				.setValue(POWERED, false)
				.setValue(RECEIVER, false);

		if (face == Direction.UP || face == Direction.DOWN) {
			Direction horiz = ctx.getHorizontalDirection().getOpposite();
			state = state.setValue(HORIZONTAL_FACING, horiz);
		} else {
			state = state.setValue(HORIZONTAL_FACING, Direction.NORTH);
		}

		return state;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction face = state.getValue(FACING);
		BlockPos support = pos.relative(face.getOpposite());
		return level.getBlockState(support).isFaceSturdy(level, support, face);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		Direction facing = state.getValue(FACING);
		return SHAPES.get(facing);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return state.getValue(RECEIVER);
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return state.getValue(RECEIVER) && state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return getSignal(state, level, pos, dir);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos,
								Block block, BlockPos fromPos, boolean isMoving) {
		if (level.isClientSide || state.getValue(RECEIVER))
			return;

		boolean powered = level.hasNeighborSignal(pos);
		if (state.getValue(POWERED) != powered)
			level.setBlock(pos, state.setValue(POWERED, powered), 3);

		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof RedstoneLinkBlockEntity link)
			link.transmitFromNeighborChange();
	}
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos,
								 Player player, InteractionHand hand, BlockHitResult hit) {
		if (player == null)
			return InteractionResult.PASS;

		ItemStack held = player.getItemInHand(hand);

		if (player.isCrouching()) {
			if (!level.isClientSide) {
				boolean receiver = !state.getValue(RECEIVER);
				level.setBlock(pos, state.setValue(RECEIVER, receiver), 3);
				if (level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity link) {
					link.updateLink();

					// ВАЖНО: сразу пересчитать сигнал
					if (receiver)
						link.updateNetworkState(); // приёмник
					else
						link.transmitFromNeighborChange(); // передатчик
				}
			}
			return InteractionResult.SUCCESS;
		}


		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof RedstoneLinkBlockEntity link))
			return InteractionResult.PASS;

		Vec3 localHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		Direction facing = state.getValue(RedstoneLinkBlock.FACING);

		double hitX, hitZ, hitY = localHit.y;
		double lx = localHit.x;
		double lz = localHit.z;

		if (facing == Direction.UP) {
			hitX = lz;
			hitZ = 1.0 - lx;
		} else if (facing == Direction.DOWN) {
			hitX = 1.0 - lz;
			hitZ = lx;
		} else {
			hitX = lx;
			hitZ = lz;
		}

		int slot = -1;

		if (facing == Direction.UP || facing == Direction.DOWN) {
			// Зоны стали шире к центру: от 0.15 до 0.45 и от 0.55 до 0.85
			if (hitX > 0.15 && hitX < 0.45 && hitZ > 0.3 && hitZ < 0.7)
				slot = 0;
			else if (hitX > 0.55 && hitX < 0.85 && hitZ > 0.3 && hitZ < 0.7)
				slot = 1;
		} else {
			// ПЕРЕСТАВЛЕНО: верхняя область теперь слот 1, нижняя — слот 0
			if (hitY > 0.55 && hitY < 0.8)
				slot = 1; // было 0
			else if (hitY > 0.2 && hitY < 0.45)
				slot = 0; // было 1
		}


		if (slot == -1)
			return InteractionResult.PASS;

		ItemStack current = link.getFrequencyInventory().getItem(slot);

		if (held.isEmpty()) {
			if (!current.isEmpty()) {
				link.getFrequencyInventory().setItem(slot, ItemStack.EMPTY);
				link.updateLink();
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.CONSUME;
		}

		if (!(held.getItem() instanceof BlockItem))
			return InteractionResult.CONSUME;

		boolean sameItem = ItemStack.isSameItemSameTags(current, held);
		boolean sameCount = current.getCount() == held.getCount();

		if (!sameItem || !sameCount) {
			link.getFrequencyInventory().setItem(slot, held.copy());
			link.updateLink();
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.CONSUME;
	}


	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return ModBlockEntities.REDSTONE_LINK.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return type == ModBlockEntities.REDSTONE_LINK.get()
				? (lvl, pos, st, be) -> ((RedstoneLinkBlockEntity) be).tick()
				: null;
	}

	private static VoxelShape rotateShape(VoxelShape shape, Direction facing, Direction horizontal) {
		VoxelShape[] buf = { shape, Shapes.empty() };
		for (AABB box : buf[0].toAabbs()) {
			AABB rotated = rotateBox(box, facing);
			buf[1] = Shapes.or(buf[1], Shapes.create(rotated));
		}
		return buf[1];
	}

	private static AABB rotateBox(AABB b, Direction facing) {
		double minX = b.minX, minY = b.minY, minZ = b.minZ;
		double maxX = b.maxX, maxY = b.maxY, maxZ = b.maxZ;

		return switch (facing) {
			case DOWN -> new AABB(minX, minZ, 1 - maxY, maxX, maxZ, 1 - minY);
			case UP -> new AABB(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
			case EAST -> new AABB(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
			case SOUTH -> new AABB(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
			case WEST -> new AABB(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
			default -> new AABB(minX, minY, minZ, maxX, maxY, maxZ); // NORTH
		};
	}
}
