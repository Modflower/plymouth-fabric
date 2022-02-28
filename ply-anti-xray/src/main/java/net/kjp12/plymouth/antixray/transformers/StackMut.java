package net.kjp12.plymouth.antixray.transformers;// Created 2021-19-12T14:51:00

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * How much to pop and push to the stack.
 * <p>
 * Meant for instances where the total weight is unsuitable, like
 * walking the ASM tree to find usages.
 *
 * @param pop  How much to pop.
 * @param push How much to push.
 * @author KJP12
 * @see Transformers#stack2(AbstractInsnNode)
 * @since ${version}
 **/
public record StackMut(int pop, int push) {
    static final StackMut
            // "he's being hit by a hammer, of course he's surprised" - Deximus-Maximus#0682
            T0_0 = new StackMut(0, 0),
            T0_1 = new StackMut(0, 1),
            T1_0 = new StackMut(1, 0),
            T1_1 = new StackMut(1, 1),
            T1_2 = new StackMut(1, 2),
            T2_0 = new StackMut(2, 0),
            T2_1 = new StackMut(2, 1),
            T2_2 = new StackMut(2, 2),
            T2_3 = new StackMut(2, 3),
            T3_0 = new StackMut(3, 0),
            Ti_0 = new StackMut(Integer.MAX_VALUE, 0);

    /**
     * Returns the weight calculated by push minus pop.
     * <p>
     * Unsuitable for use by testing for consumers.
     */
    public int weight() {
        return -pop + push;
    }

    /**
     * Tests if the instruction will consume the item on stack.
     *
     * @param stack The current stack size.
     * @return If the item would be popped.
     */
    public boolean pop(int stack) {
        return stack - pop <= 0;
    }

    /**
     * Tests if the instruction will produce the item on stack.
     *
     * @param stack The current stack size.
     * @return If the item would be pushed.
     */
    public boolean push(int stack) {
        return stack - push <= 0;
    }
}
