package com.zoritism.wirelesslinks.foundation.blockEntity.behaviour;

import java.util.ConcurrentModificationException;

import com.zoritism.wirelesslinks.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockEntityBehaviour {

	protected SmartBlockEntity blockEntity;
	protected final BehaviourType<?> type;
	private int lazyTickRate;
	private int lazyTickCounter;

	public BlockEntityBehaviour(BehaviourType<?> type, SmartBlockEntity be) {
		this.type = type;
		this.blockEntity = be;
		setLazyTickRate(10);
	}

	public abstract BehaviourType<?> getType();

	public void initialize() {}

	public void tick() {
		if (lazyTickCounter-- <= 0) {
			lazyTickCounter = lazyTickRate;
			lazyTick();
		}
	}

	public void read(CompoundTag nbt, boolean clientPacket) {}

	public void write(CompoundTag nbt, boolean clientPacket) {}

	public void writeSafe(CompoundTag nbt) {
		write(nbt, false);
	}

	public boolean isSafeNBT() {
		return false;
	}

	public void onBlockChanged(BlockState oldState) {}

	public void onNeighborChanged(BlockPos neighborPos) {}

	public void unload() {}

	public void destroy() {}

	public void setLazyTickRate(int slowTickRate) {
		this.lazyTickRate = slowTickRate;
		this.lazyTickCounter = slowTickRate;
	}

	public void lazyTick() {}

	public BlockPos getPos() {
		return blockEntity.getBlockPos();
	}

	public Level getWorld() {
		return blockEntity.getLevel();
	}

	/** Позволяет назначить поведение конкретному SmartBlockEntity после его создания */
	public void attachTo(SmartBlockEntity be) {
		this.blockEntity = be;
	}

	public static <T extends BlockEntityBehaviour> T get(BlockGetter reader, BlockPos pos, BehaviourType<T> type) {
		BlockEntity be;
		try {
			be = reader.getBlockEntity(pos);
		} catch (ConcurrentModificationException e) {
			be = null;
		}
		return get(be, type);
	}

	public static <T extends BlockEntityBehaviour> T get(BlockEntity be, BehaviourType<T> type) {
		if (be == null)
			return null;
		if (!(be instanceof SmartBlockEntity ste))
			return null;
		return ste.getBehaviour(type);
	}
}
