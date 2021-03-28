package net.kjp12.helium.helpers;

import net.minecraft.network.PacketByteBuf;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * @author KJP12
 * @since 0.0.0
 **/
public class AtomicPackedIntegerArray {
    private final byte[] raw;
    private final int bits, longSize;

    private static final VarHandle rawHandle = MethodHandles.byteArrayViewVarHandle(byte[].class, ByteOrder.nativeOrder());

    public AtomicPackedIntegerArray(int bits, int size) {
        this.bits = bits;
        this.raw = new byte[(size >>> 3) * bits];
        this.longSize = raw.length >>> 3;
    }

    public int get(int index) {

        return 0;
    }

    public void set(int index, int value) {
    }

    public int setAndGetOldValue(int index, int value) {
        return 0;
    }

    public void toPacket(PacketByteBuf buf) {
        buf.writeVarInt(longSize);
        for (int i = 0; i < longSize; i++) {
            buf.writeLong((long) rawHandle.getOpaque(raw, i));
        }
    }
}
