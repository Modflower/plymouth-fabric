package net.kjp12.plymouth.antixray.transformers;// Created 2021-19-12T14:51:00

/**
 * @author KJP12
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
            T3_0 = new StackMut(3, 0);


    public int weight() {
        return -pop + push;
    }

    public boolean pop(int stack) {
        return stack - pop <= 0;
    }
}
