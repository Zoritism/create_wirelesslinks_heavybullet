package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

// LOGGING
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedstoneLinkFrequency {

    private static final Logger LOGGER = LogManager.getLogger();

    public static class Frequency {
        private final ItemStack stack;

        private Frequency(ItemStack stack) {
            this.stack = stack.copy();
            LOGGER.info("[Frequency][constructor] stack={} count={} nbt={}", stack, stack == null ? 0 : stack.getCount(), stack == null ? null : stack.getTag());
        }

        public static Frequency of(ItemStack stack) {
            Frequency f = new Frequency(stack == null ? ItemStack.EMPTY : stack);
            LOGGER.info("[Frequency][of] stack={} -> freq={}, count={}, nbt={}, hash={}", stack, f, f.stack.getCount(), f.stack.getTag(), f.hashCode());
            return f;
        }

        public boolean isEmpty() {
            boolean empty = stack.isEmpty();
            LOGGER.info("[Frequency][isEmpty] freq={} stack={} result={}", this, stack, empty);
            return empty;
        }

        public ItemStack getStack() {
            LOGGER.info("[Frequency][getStack] freq={} stack={}", this, stack);
            return stack;
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            stack.save(tag);
            LOGGER.info("[Frequency][serialize] freq={} stack={} tag={}", this, stack, tag);
            return tag;
        }

        public static Frequency deserialize(CompoundTag tag) {
            Frequency f = new Frequency(ItemStack.of(tag));
            LOGGER.info("[Frequency][deserialize] tag={} -> freq={}, count={}, nbt={}, hash={}", tag, f, f.stack.getCount(), f.stack.getTag(), f.hashCode());
            return f;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Frequency other))
                return false;

            if (stack.isEmpty() && other.stack.isEmpty()) {
                LOGGER.info("[Frequency][equals] this={} other={} -> true (both empty)", this, other);
                return true;
            }

            boolean eq = ItemStack.isSameItemSameTags(stack, other.stack) && stack.getCount() == other.stack.getCount();
            if (!eq) {
                LOGGER.info("[Frequency][equals] this={} (item={},count={},nbt={}) other={} (item={},count={},nbt={}) -> false",
                        this, stack.getItem(), stack.getCount(), stack.getTag(),
                        other, other.stack.getItem(), other.stack.getCount(), other.stack.getTag());
            } else {
                LOGGER.info("[Frequency][equals] this={} other={} -> true", this, other);
            }
            return eq;
        }

        @Override
        public int hashCode() {
            if (stack.isEmpty()) {
                LOGGER.info("[Frequency][hashCode] freq={} stack is empty -> 0", this);
                return 0;
            }
            int hash = Objects.hash(stack.getItem(), stack.getTag(), stack.getCount());
            LOGGER.info("[Frequency][hashCode] freq={} item={} count={} nbt={} -> hash={}", this, stack.getItem(), stack.getCount(), stack.getTag(), hash);
            return hash;
        }

        @Override
        public String toString() {
            return "Frequency[" + stack + "]";
        }
    }

    public static class FrequencyPair {
        private final Frequency first;
        private final Frequency second;

        public FrequencyPair(Frequency first, Frequency second) {
            this.first = first == null ? Frequency.of(ItemStack.EMPTY) : first;
            this.second = second == null ? Frequency.of(ItemStack.EMPTY) : second;
            LOGGER.info("[FrequencyPair][constructor] first={} second={}", this.first, this.second);
        }

        public static FrequencyPair of(ItemStack a, ItemStack b) {
            FrequencyPair p = new FrequencyPair(Frequency.of(a), Frequency.of(b));
            LOGGER.info("[FrequencyPair][of] a={} b={} -> pair={} hash={}", a, b, p, p.hashCode());
            return p;
        }

        public Frequency getFirst() {
            LOGGER.info("[FrequencyPair][getFirst] pair={} first={}", this, first);
            return first;
        }

        public Frequency getSecond() {
            LOGGER.info("[FrequencyPair][getSecond] pair={} second={}", this, second);
            return second;
        }

        public Couple<Frequency> toCouple() {
            Couple<Frequency> c = Couple.of(first, second);
            LOGGER.info("[FrequencyPair][toCouple] pair={} -> couple={} hash={}", this, c, c.hashCode());
            return c;
        }

        public boolean isEmpty() {
            boolean empty = first.isEmpty() && second.isEmpty();
            LOGGER.info("[FrequencyPair][isEmpty] pair={} result={}", this, empty);
            return empty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FrequencyPair that)) return false;
            boolean eq = Objects.equals(first, that.first) && Objects.equals(second, that.second);
            if (!eq) {
                LOGGER.info("[FrequencyPair][equals] this={} other={} -> false", this, that);
            } else {
                LOGGER.info("[FrequencyPair][equals] this={} other={} -> true", this, that);
            }
            return eq;
        }

        @Override
        public int hashCode() {
            int hash = Objects.hash(first, second);
            LOGGER.info("[FrequencyPair][hashCode] pair={} -> hash={}", this, hash);
            return hash;
        }

        @Override
        public String toString() {
            return "FrequencyPair[" + first + ", " + second + "]";
        }
    }
}