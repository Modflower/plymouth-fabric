package net.kjp12.plymouth.database;// Created 2021-07-05T15:56:59

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Objects;

/**
 * Custom item stack hasher that only checks the hashcode of the underlying item and NBT, and if the stacks can stack.
 *
 * @author KJP12
 * @since ${version}
 **/
public final class ItemStackHasher implements Hash.Strategy<ItemStack> {
    public static final ItemStackHasher INSTANCE = new ItemStackHasher();

    /**
     * @param o The stack to hash.
     * @return If o is not null, standard hashcode of ItemStack, else 0.
     */
    @Override
    public int hashCode(ItemStack o) {
        return o == null ? 0 : 31 * o.getItem().hashCode() + Objects.hashCode(o.getNbt());
    }

    /**
     * @param i The item to hash.
     * @return If i is not null, standard hashcode of Item multiplied by 31 as if it was a stack that had no NBT, else 0.
     */
    public static int hashCode(Item i) {
        return i == null ? 0 : 31 * i.hashCode();
    }

    /**
     * @param a First stack of the equality test.
     * @param b Second stack of the equality test.
     * @return true only if a is b or if a is not null, b is not null <em>and</em> a can combine into b.
     */
    @Override
    public boolean equals(ItemStack a, ItemStack b) {
        return a == b || a != null && b != null && ItemStack.canCombine(a, b);
    }

    /**
     * Stack combination test assuming <code>a</code> is a lone item in a stack without NBT.
     *
     * @param a Item of the equality test.
     * @param b Stack of the equality test.
     * @return true only if <code>b</code> is not null <em>and</em> if the underlying item in <code>b</code> equals <code>a</code> and <code>b</code> does not have any NBT.
     */
    public static boolean equals(Item a, ItemStack b) {
        return b != null && a == b.getItem() && !b.hasNbt();
    }
}
