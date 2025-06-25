package com.zoritism.wirelesslinks.foundation.blockEntity;

import com.zoritism.wirelesslinks.foundation.blockEntity.behaviour.BehaviourType;
import com.zoritism.wirelesslinks.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.*;
import java.util.function.Consumer;

public abstract class SmartBlockEntity extends BlockEntity {

	private final Map<BehaviourType<?>, BlockEntityBehaviour> behaviours = new Reference2ObjectArrayMap<>();
	private boolean initialized = false;
	private boolean firstNbtRead = true;
	protected int lazyTickRate;
	protected int lazyTickCounter;
	private boolean chunkUnloaded;

	private boolean virtualMode;

	public SmartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
		ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
		addBehaviours(list);
		list.forEach(b -> behaviours.put(b.getType(), b));
	}

	public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

	public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {}

	public void initialize() {
		if (firstNbtRead) {
			firstNbtRead = false;
		}
		forEachBehaviour(BlockEntityBehaviour::initialize);
		lazyTick();
	}

	public void tick() {
		if (!initialized && hasLevel()) {
			initialize();
			initialized = true;
		}
		if (lazyTickCounter-- <= 0) {
			lazyTickCounter = lazyTickRate;
			lazyTick();
		}
		forEachBehaviour(BlockEntityBehaviour::tick);
	}

	public void lazyTick() {}

	protected void write(CompoundTag tag, boolean clientPacket) {
		saveAdditional(tag);
		forEachBehaviour(tb -> tb.write(tag, clientPacket));
	}

	public void writeSafe(CompoundTag tag) {
		saveAdditional(tag);
		forEachBehaviour(tb -> {
			if (tb.isSafeNBT())
				tb.writeSafe(tag);
		});
	}

	protected void read(CompoundTag tag, boolean clientPacket) {
		if (firstNbtRead) {
			firstNbtRead = false;
			ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
			addBehavioursDeferred(list);
			list.forEach(b -> behaviours.put(b.getType(), b));
		}
		load(tag);
		forEachBehaviour(tb -> tb.read(tag, clientPacket));
	}

	@Override
	public final void load(CompoundTag tag) {
		super.load(tag);
		read(tag, false);
	}

	@Override
	public final void saveAdditional(CompoundTag tag) {
		write(tag, false);
	}

	public final void readClient(CompoundTag tag) {
		read(tag, true);
	}

	public final CompoundTag writeClient(CompoundTag tag) {
		write(tag, true);
		return tag;
	}

	public void onChunkUnloaded() {
		chunkUnloaded = true;
	}

	@Override
	public final void setRemoved() {
		super.setRemoved();
		if (!chunkUnloaded)
			remove();
		invalidate();
	}

	public void invalidate() {
		forEachBehaviour(BlockEntityBehaviour::unload);
	}

	public void remove() {}

	public void destroy() {
		forEachBehaviour(BlockEntityBehaviour::destroy);
	}

	@SuppressWarnings("unchecked")
	public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T> type) {
		return (T) behaviours.get(type);
	}

	public void forEachBehaviour(Consumer<BlockEntityBehaviour> action) {
		getAllBehaviours().forEach(action);
	}

	public Collection<BlockEntityBehaviour> getAllBehaviours() {
		return behaviours.values();
	}

	public void attachBehaviourLate(BlockEntityBehaviour behaviour) {
		behaviours.put(behaviour.getType(), behaviour);
		behaviour.attachTo(this);
		behaviour.initialize();
	}

	public void removeBehaviour(BehaviourType<?> type) {
		BlockEntityBehaviour remove = behaviours.remove(type);
		if (remove != null) {
			remove.unload();
		}
	}

	public void setLazyTickRate(int slowTickRate) {
		this.lazyTickRate = slowTickRate;
		this.lazyTickCounter = slowTickRate;
	}

	public void markVirtual() {
		virtualMode = true;
	}

	public boolean isVirtual() {
		return virtualMode;
	}

	public boolean isChunkUnloaded() {
		return chunkUnloaded;
	}

	public boolean canPlayerUse(Player player) {
		if (level == null || level.getBlockEntity(worldPosition) != this)
			return false;
		return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
				worldPosition.getZ() + 0.5D) <= 64.0D;
	}

	public void sendToMenu(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(getBlockPos());
		buffer.writeNbt(getUpdateTag());
	}

	@SuppressWarnings("deprecation")
	public void refreshBlockState() {
		setBlockState(getLevel().getBlockState(getBlockPos()));
	}

	protected boolean isItemHandlerCap(Capability<?> cap) {
		return cap == ForgeCapabilities.ITEM_HANDLER;
	}

	protected boolean isFluidHandlerCap(Capability<?> cap) {
		return cap == ForgeCapabilities.FLUID_HANDLER;
	}
}
