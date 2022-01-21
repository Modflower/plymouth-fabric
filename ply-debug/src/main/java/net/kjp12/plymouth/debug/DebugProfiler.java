package net.kjp12.plymouth.debug;// Created 2021-03-29T00:14:15

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

/**
 * @author KJP12
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugProfiler {
    private final long[] points;
    private final int r, g, b;
    private final float m, m1, s;
    private final int mask;
    private int index;

    public DebugProfiler(int length, int r, int g, int b, float m, float s) {
        int len = 1 << MathHelper.ceilLog2(length);
        this.points = new long[len];
        mask = len - 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.m = -m;
        this.m1 = m + 1;
        this.s = s;
    }

    public DebugProfiler(int length, int r, int g, int b, float m) {
        this(length, r, g, b, m, 1F);
    }

    public void render(MatrixStack stack) {
        stack.push();
        stack.scale(s, s, s);
        for (int i = 0; i < points.length; i++) {
            // stack.push();
            long pos = points[i];
            int x = BlockPos.unpackLongX(pos);
            int y = BlockPos.unpackLongY(pos);
            int z = BlockPos.unpackLongZ(pos);
            // stack.translate(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos));
            RenderBatch.drawWireBox(stack.peek(), m + x, m + y, m + z, m1 + x, m1 + y, m1 + z, r, g, b, (int) ((i + 1 == (index & mask) ? 0.75F : 0.25F * ((float) ((i - index) & mask) / points.length)) * 255F), false); //, false, false, false);
            //WorldRenderer.drawBox(stack, consumer, m, m, m, m1, m1, m1, r, g, b, i + 1 == (index & mask) ? 0.75F : 0.25F * ((float) ((i - index) & mask) / points.length));
            // stack.pop();
        }
        stack.pop();
    }

    public void push(long point) {
        points[index++ & mask] = point;
    }

    public void clear() {
        Arrays.fill(points, 0);
        index = 0;
    }
}
