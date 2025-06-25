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

        // Если элементы — ItemStack, сравниваем без учёта NBT
        if (first instanceof ItemStack f1 && second instanceof ItemStack s1
                && other.first instanceof ItemStack f2 && other.second instanceof ItemStack s2) {
            return itemStackEqualsIgnoreNBT(f1, f2) && itemStackEqualsIgnoreNBT(s1, s2);
        }

        return Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }

    private boolean itemStackEqualsIgnoreNBT(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getCount() == b.getCount();
    }

    @Override
    public int hashCode() {
        if (first instanceof ItemStack f && second instanceof ItemStack s) {
            return Objects.hash(f.getItem(), f.getCount(), s.getItem(), s.getCount());
        }
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Couple[" + first + ", " + second + "]";
    }
}
