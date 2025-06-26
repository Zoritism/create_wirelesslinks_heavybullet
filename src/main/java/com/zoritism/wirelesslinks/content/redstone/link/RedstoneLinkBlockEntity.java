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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedstoneLinkBlockEntity extends BlockEntity implements IRedstoneLinkable {

	private static final Logger LOGGER = LogManager.getLogger();

	private boolean receivedSignalChanged;
	private int receivedSignal;
	private int transmittedSignal;
	private boolean transmitter;

	private final SimpleContainer frequencyInv = new SimpleContainer(2);

	// Для хранения частоты, чтобы отписаться при смене
	private Couple<Frequency> lastNetworkKey = null;

	public RedstoneLinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REDSTONE_LINK.get(), pos, state);
	}

	@Override
	public void onLoad() {
		LOGGER.info("[RedstoneLinkBlockEntity][onLoad] pos={}, isClient={}", worldPosition, level != null ? level.isClientSide : "null");
		updateLink();
	}

	public void tick() {
		if (level == null || level.isClientSide)
			return;

		boolean prevTransmitter = transmitter;
		transmitter = isTransmitterBlock();

		if (prevTransmitter != transmitter) {
			LOGGER.info("[RedstoneLinkBlockEntity][tick] pos={} transmitter state changed to {}, updating link", worldPosition, transmitter);
			updateLink();
			return;
		}

		// ВАЖНО: transmitter реально передает сигнал
		if (transmitter) {
			boolean powered = level.hasNeighborSignal(worldPosition);
			int newSignal = powered ? 15 : 0;
			if (transmittedSignal != newSignal) {
				transmit(newSignal);
			}
			return;
		}

		boolean powered = receivedSignal > 0;
		BlockState state = getBlockState();
		if (state.getValue(RedstoneLinkBlock.POWERED) != powered) {
			receivedSignalChanged = true;
			LOGGER.info("[RedstoneLinkBlockEntity][tick] pos={} changing blockstate POWERED to {}", worldPosition, powered);
			level.setBlockAndUpdate(worldPosition, state.setValue(RedstoneLinkBlock.POWERED, powered));
		}

		if (receivedSignalChanged)
			propagateNeighbourUpdates(state);
	}

	private boolean isTransmitterBlock() {
		boolean result = !getBlockState().getValue(RedstoneLinkBlock.RECEIVER);
		LOGGER.info("[RedstoneLinkBlockEntity][isTransmitterBlock] pos={} isTransmitter={}", worldPosition, result);
		return result;
	}

	public void updateLink() {
		if (level == null || level.isClientSide)
			return;

		Couple<Frequency> newKey = getNetworkKey();
		if (lastNetworkKey != null && !lastNetworkKey.equals(newKey)) {
			LOGGER.info("[RedstoneLinkBlockEntity][updateLink] pos={} removing from old key={}", worldPosition, lastNetworkKey);
			WirelessLinkNetworkHandler.removeFromNetwork(level, this);
		}
		lastNetworkKey = newKey;

		LOGGER.info("[RedstoneLinkBlockEntity][updateLink] pos={} freq={} transmitter={}", worldPosition, getFrequency(), transmitter);

		WirelessLinkNetworkHandler.addToNetwork(level, this);

		LinkHandler.get(level).updateLink(this);
		LinkHandler.get(level).refreshChannel(getFrequency());

		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	private void propagateNeighbourUpdates(BlockState state) {
		BlockPos back = worldPosition.relative(state.getValue(RedstoneLinkBlock.FACING).getOpposite());
		level.blockUpdated(worldPosition, state.getBlock());
		level.blockUpdated(back, level.getBlockState(back).getBlock());
		receivedSignalChanged = false;
		LOGGER.info("[RedstoneLinkBlockEntity][propagateNeighbourUpdates] pos={} blockUpdated for pos={} and back={}", worldPosition, worldPosition, back);
	}

	public void transmit(int strength) {
		if (!transmitter || level == null)
			return;

		LOGGER.info("[RedstoneLinkBlockEntity][transmit] pos={} transmitter={}, oldSignal={}, newSignal={}", worldPosition, transmitter, transmittedSignal, strength);

		if (transmittedSignal != strength) {
			transmittedSignal = strength;
			LinkHandler.get(level).refreshChannel(getFrequency());
			WirelessLinkNetworkHandler.updateNetwork(level, getNetworkKey());
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		LOGGER.info("[RedstoneLinkBlockEntity][setRemoved] pos={}", worldPosition);
		if (level != null && !level.isClientSide) {
			WirelessLinkNetworkHandler.removeFromNetwork(level, this);
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
		LOGGER.info("[RedstoneLinkBlockEntity][saveAdditional] pos={} transmitter={} receivedSignal={} transmittedSignal={}", worldPosition, transmitter, receivedSignal, transmittedSignal);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		transmitter = tag.getBoolean("Tx");
		receivedSignal = tag.getInt("Recv");
		transmittedSignal = tag.getInt("TxPow");

		for (int i = 0; i < 2; i++)
			frequencyInv.setItem(i, ItemStack.of(tag.getCompound("Freq" + i)));

		LOGGER.info("[RedstoneLinkBlockEntity][load] pos={} transmitter={} receivedSignal={} transmittedSignal={}", worldPosition, transmitter, receivedSignal, transmittedSignal);
		updateLink(); // гарантирует корректную регистрацию после загрузки
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		for (int i = 0; i < 2; i++) {
			CompoundTag stackTag = new CompoundTag();
			frequencyInv.getItem(i).save(stackTag);
			tag.put("Freq" + i, stackTag);
		}
		LOGGER.info("[RedstoneLinkBlockEntity][getUpdateTag] pos={}", worldPosition);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		super.handleUpdateTag(tag);
		for (int i = 0; i < 2; i++) {
			frequencyInv.setItem(i, ItemStack.of(tag.getCompound("Freq" + i)));
		}
		LOGGER.info("[RedstoneLinkBlockEntity][handleUpdateTag] pos={}", worldPosition);
		updateLink(); // гарантирует корректную регистрацию после client sync
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		LOGGER.info("[RedstoneLinkBlockEntity][getUpdatePacket] pos={}", worldPosition);
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		LOGGER.info("[RedstoneLinkBlockEntity][onDataPacket] pos={}", worldPosition);
		handleUpdateTag(pkt.getTag());
	}

	@Override
	public BlockPos getLocation() { return worldPosition; }

	@Override
	public Level getLevel() { return level; }

	@Override
	public int getTransmittedStrength() { return transmittedSignal; }

	@Override
	public void setReceivedStrength(int power) {
		LOGGER.info("[RedstoneLinkBlockEntity][setReceivedStrength] pos={} oldReceivedSignal={} newReceivedSignal={}", worldPosition, receivedSignal, power);
		receivedSignal = power;
	}

	@Override
	public boolean isListening() { return !transmitter; }

	@Override
	public Couple<ItemStack> getFrequency() {
		Couple<ItemStack> freq = Couple.of(normalize(frequencyInv.getItem(0)),
				normalize(frequencyInv.getItem(1)));
		LOGGER.info("[RedstoneLinkBlockEntity][getFrequency] pos={} freq={}", worldPosition, freq);
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
		LOGGER.info("[RedstoneLinkBlockEntity][transmitFromNeighborChange] pos={} powered={}", worldPosition, powered);
		transmit(powered ? 15 : 0);
	}

	public void updateNetworkState() {
		if (level == null || transmitter)
			return;

		LOGGER.info("[RedstoneLinkBlockEntity][updateNetworkState] pos={}", worldPosition);
		tick();
	}

	public ItemStack getLeftFrequencyStack() {
		ItemStack stack = frequencyInv.getItem(0);
		LOGGER.info("[RedstoneLinkBlockEntity][getLeftFrequencyStack] pos={} stack={}", worldPosition, stack);
		return stack;
	}

	public ItemStack getRightFrequencyStack() {
		ItemStack stack = frequencyInv.getItem(1);
		LOGGER.info("[RedstoneLinkBlockEntity][getRightFrequencyStack] pos={} stack={}", worldPosition, stack);
		return stack;
	}

	public Direction getHorizontalFacing() {
		BlockState state = getBlockState();
		if (state.hasProperty(RedstoneLinkBlock.HORIZONTAL_FACING)) {
			Direction dir = state.getValue(RedstoneLinkBlock.HORIZONTAL_FACING);
			LOGGER.info("[RedstoneLinkBlockEntity][getHorizontalFacing] pos={} dir={}", worldPosition, dir);
			return dir;
		}
		LOGGER.info("[RedstoneLinkBlockEntity][getHorizontalFacing] pos={} dir=NORTH (default)", worldPosition);
		return Direction.NORTH;
	}

	public SimpleContainer getFrequencyInventory() {
		LOGGER.info("[RedstoneLinkBlockEntity][getFrequencyInventory] pos={}", worldPosition);
		return frequencyInv;
	}
}