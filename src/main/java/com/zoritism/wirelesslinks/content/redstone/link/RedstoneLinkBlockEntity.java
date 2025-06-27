package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.content.redstone.link.RedstoneLinkFrequency.Frequency;
import com.zoritism.wirelesslinks.registry.ModBlockEntities;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Минималистичная реализация RedstoneLinkBlockEntity, максимально приближённая к Create.
 * Без логирования.
 */
public class RedstoneLinkBlockEntity extends BlockEntity implements IRedstoneLinkable {

	private boolean receivedSignalChanged;
	private int receivedSignal;
	private int transmittedSignal;
	private boolean transmitter;

	private final SimpleContainer frequencyInv = new SimpleContainer(2);

	public RedstoneLinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REDSTONE_LINK.get(), pos, state);
	}

	@Override
	public void onLoad() {
		if (level != null && !level.isClientSide)
			updateLink();
	}

	public void tick() {
		if (level == null || level.isClientSide)
			return;

		boolean nowTx = isTransmitterBlock();
		if (nowTx != transmitter) {
			transmitter = nowTx;
			updateLink();
			return;
		}

		if (transmitter)
			return;

		boolean powered = receivedSignal > 0;
		BlockState state = getBlockState();
		if (state.getValue(RedstoneLinkBlock.POWERED) != powered) {
			receivedSignalChanged = true;
			level.setBlockAndUpdate(worldPosition, state.setValue(RedstoneLinkBlock.POWERED, powered));
		}

		if (receivedSignalChanged) {
			propagateNeighbourUpdates(state);
			receivedSignalChanged = false;
		}
	}

	private boolean isTransmitterBlock() {
		return !getBlockState().getValue(RedstoneLinkBlock.RECEIVER);
	}

	public void updateLink() {
		if (level == null || level.isClientSide)
			return;

		LinkHandler.get(level).updateLink(this);
		LinkHandler.get(level).refreshChannel(getFrequency());

		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	private void propagateNeighbourUpdates(BlockState state) {
		BlockPos back = worldPosition.relative(state.getValue(RedstoneLinkBlock.FACING).getOpposite());
		level.blockUpdated(worldPosition, state.getBlock());
		level.blockUpdated(back, level.getBlockState(back).getBlock());
	}

	public void transmit(int strength) {
		if (!transmitter || level == null)
			return;

		if (transmittedSignal != strength) {
			transmittedSignal = strength;
			LinkHandler.get(level).refreshChannel(getFrequency());
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide) {
			LinkHandler.get(level).removeLink(this);
			LinkHandler.get(level).refreshChannel(getFrequency());
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("Tx", transmitter);
		tag.putInt("Recv", receivedSignal);
		tag.putInt("TxPow", transmittedSignal);

		for (int i = 0; i < 2; i++) {
			CompoundTag stackTag = new CompoundTag();
			frequencyInv.getItem(i).save(stackTag);
			tag.put("Freq" + i, stackTag);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		transmitter = tag.getBoolean("Tx");
		receivedSignal = tag.getInt("Recv");
		transmittedSignal = tag.getInt("TxPow");

		for (int i = 0; i < 2; i++)
			frequencyInv.setItem(i, ItemStack.of(tag.getCompound("Freq" + i)));

		if (level != null && !level.isClientSide)
			updateLink();
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		for (int i = 0; i < 2; i++) {
			CompoundTag stackTag = new CompoundTag();
			frequencyInv.getItem(i).save(stackTag);
			tag.put("Freq" + i, stackTag);
		}
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		super.handleUpdateTag(tag);
		for (int i = 0; i < 2; i++) {
			frequencyInv.setItem(i, ItemStack.of(tag.getCompound("Freq" + i)));
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		handleUpdateTag(pkt.getTag());
	}

	@Override
	public BlockPos getLocation()             { return worldPosition; }
	@Override
	public Level getLevel()                   { return level; }
	@Override
	public int getTransmittedStrength()       { return transmittedSignal; }

	@Override
	public void setReceivedStrength(int power) {
		if (receivedSignal != power) {
			receivedSignal = power;
			receivedSignalChanged = true;
			tick(); // сразу обновить POWERED и соседей
		} else {
			// ВАЖНО: даже если сигнал не изменился, если power == 0 и block всё ещё POWERED, форсируем обновление!
			if (power == 0) {
				BlockState state = getBlockState();
				if (state.hasProperty(RedstoneLinkBlock.POWERED) && state.getValue(RedstoneLinkBlock.POWERED)) {
					receivedSignalChanged = true;
					tick();
				}
			}
		}
	}

	@Override
	public boolean isListening()              { return !transmitter; }

	@Override
	public Couple<ItemStack> getFrequency() {
		Couple<ItemStack> freq = Couple.of(normalize(frequencyInv.getItem(0)),
				normalize(frequencyInv.getItem(1)));
		return freq;
	}

	@Override
	@Deprecated
	public Couple<Frequency> getNetworkKey() {
		return Couple.of(Frequency.of(frequencyInv.getItem(0)),
				Frequency.of(frequencyInv.getItem(1)));
	}

	private static ItemStack normalize(ItemStack in) {
		if (in.isEmpty())
			return ItemStack.EMPTY;

		ItemStack copy = in.copy();
		copy.setTag(null);
		return copy;
	}

	public void transmitFromNeighborChange() {
		if (!transmitter || level == null)
			return;

		boolean powered = getBlockState().getValue(RedstoneLinkBlock.POWERED);
		transmit(powered ? 15 : 0);
	}

	public void updateNetworkState() {
		if (level == null || transmitter)
			return;

		tick();
	}

	public ItemStack getLeftFrequencyStack() {
		return frequencyInv.getItem(0);
	}

	public ItemStack getRightFrequencyStack() {
		return frequencyInv.getItem(1);
	}

	public Direction getHorizontalFacing() {
		BlockState state = getBlockState();
		if (state.hasProperty(RedstoneLinkBlock.HORIZONTAL_FACING)) {
			return state.getValue(RedstoneLinkBlock.HORIZONTAL_FACING);
		}
		return Direction.NORTH;
	}

	public SimpleContainer getFrequencyInventory() {
		return frequencyInv;
	}
}