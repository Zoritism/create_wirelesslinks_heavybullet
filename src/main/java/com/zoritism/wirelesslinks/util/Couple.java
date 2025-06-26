package com.zoritism.wirelesslinks.util;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class Couple<T> {

    private final T first;
    private final T second;

    private Couple(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public static <T> Couple<T> of(T first, T second) {
        return new Couple<>(first, second);
    }

    public T getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    public boolean contains(T value) {
        return Objects.equals(first, value) || Objects.equals(second, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Couple<?> other))
            return false;

        // Если элементы — ItemStack, сравниваем как Frequency: isSameItemSameTags + count
        if (first instanceof ItemStack f1 && second instanceof ItemStack s1
                && other.first instanceof ItemStack f2 && other.second instanceof ItemStack s2) {
            // Считаем все пустые стеки одинаковыми частотами
            boolean leftEmpty = f1.isEmpty() && f2.isEmpty();
            boolean rightEmpty = s1.isEmpty() && s2.isEmpty();
            if (leftEmpty && rightEmpty)
                return true;

            return ItemStack.isSameItemSameTags(f1, f2) && f1.getCount() == f2.getCount()
                    && ItemStack.isSameItemSameTags(s1, s2) && s1.getCount() == s2.getCount();
        }

        return Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }

    @Override
    public int hashCode() {
        if (first instanceof ItemStack f && second instanceof ItemStack s) {
            // Совместимо с Frequency.hashCode
            int left = f.isEmpty() ? 0 : Objects.hash(f.getItem(), f.getTag(), f.getCount());
            int right = s.isEmpty() ? 0 : Objects.hash(s.getItem(), s.getTag(), s.getCount());
            return Objects.hash(left, right);
        }
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Couple[" + first + ", " + second + "]";
    }
}