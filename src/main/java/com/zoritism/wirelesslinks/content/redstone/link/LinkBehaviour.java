package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.foundation.blockEntity.SmartBlockEntity;
import com.zoritism.wirelesslinks.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.zoritism.wirelesslinks.foundation.blockEntity.behaviour.BehaviourType;
import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.world.level.Level;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class LinkBehaviour extends BlockEntityBehaviour {

    public static final BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

    private final IRedstoneLinkable owner;
    private final boolean isReceiver;

    private final IntSupplier signalGetter;
    private final IntConsumer signalSetter;

    public static LinkBehaviour transmitter(SmartBlockEntity be, IRedstoneLinkable owner, IntSupplier getter) {
        return new LinkBehaviour(be, owner, false, getter, null);
    }

    public static LinkBehaviour receiver(SmartBlockEntity be, IRedstoneLinkable owner, IntConsumer setter) {
        return new LinkBehaviour(be, owner, true, null, setter);
    }

    private LinkBehaviour(SmartBlockEntity be, IRedstoneLinkable owner, boolean isReceiver,
                          IntSupplier signalGetter, IntConsumer signalSetter) {
        super(TYPE, be);
        this.owner = owner;
        this.isReceiver = isReceiver;
        this.signalGetter = signalGetter;
        this.signalSetter = signalSetter;

        attach();
    }

    public void attach() {
        Level level = getLevel();
        if (level != null)
            WirelessLinkNetworkHandler.addToNetwork(level, owner);
    }

    public void detach() {
        Level level = getLevel();
        if (level != null)
            WirelessLinkNetworkHandler.removeFromNetwork(level, owner);
    }

    public void notifySignalChange() {
        if (!isReceiver) {
            Level level = getLevel();
            if (level != null)
                WirelessLinkNetworkHandler.updateNetwork(level, owner.getNetworkKey());
        }
    }

    public void updateReceivedSignal(int power) {
        if (isReceiver && signalSetter != null)
            signalSetter.accept(power);
    }

    private Level getLevel() {
        return owner.getLevel();
    }

    public int getSignal() {
        return signalGetter != null ? signalGetter.getAsInt() : 0;
    }

    public Couple<RedstoneLinkFrequency.Frequency> getKey() {
        return owner.getNetworkKey();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
