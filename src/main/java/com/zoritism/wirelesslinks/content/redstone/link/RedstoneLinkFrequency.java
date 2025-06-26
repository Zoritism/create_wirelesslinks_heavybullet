package com.zoritism.wirelesslinks.content.redstone.link;

import com.zoritism.wirelesslinks.util.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class RedstoneLinkFrequency {

    public static class Frequency {
        private final ItemStack stack;

        private Frequency(ItemStack stack) {
            this.stack = stack.copy();
            this.stack.setCount(1); // нормализуем стек
        }

        public static Frequency of(ItemStack stack) {
            return new Frequency(stack == null ? ItemStack.EMPTY : stack);
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        public ItemStack getStack() {
            return stack;
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            stack.save(tag);
            return tag;
        }

        public static Frequency deserialize(CompoundTag tag) {
            return new Frequency(ItemStack.of(tag));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Frequency other))
                return false;

            // Считаем все пустые стеки одинаковыми частотами
            if (stack.isEmpty() && other.stack.isEmpty())
                return true;

            return ItemStack.isSameItemSameTags(stack, other.stack);
        }

        @Override
        public int hashCode() {
            if (stack.isEmpty())
                return 0;
            return Objects.hash(stack.getItem(), stack.getTag());
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
        }

        public static FrequencyPair of(ItemStack a, ItemStack b) {
            return new FrequencyPair(Frequency.of(a), Frequency.of(b));
        }

        public Frequency getFirst() {
            return first;
        }

        public Frequency getSecond() {
            return second;
        }

        public Couple<Frequency> toCouple() {
            return Couple.of(first, second);
        }

        public boolean isEmpty() {
            return first.isEmpty() && second.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FrequencyPair that)) return false;
            return Objects.equals(first, that.first) && Objects.equals(second, that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public String toString() {
            return "FrequencyPair[" + first + ", " + second + "]";
        }
    }
}
