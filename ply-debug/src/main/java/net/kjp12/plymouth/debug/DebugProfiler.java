package net.kjp12.plymouth.debug;// Created 2021-03-29T00:14:15

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * @author KJP12
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugProfiler {
    private final long[] points;
    private final float r, g, b, s, s1;
    private final int mask;
    private int index;

    public DebugProfiler(int length, float r, float g, float b, float s) {
        int len = 1 << MathHelper.log2DeBruijn(length);
        this.points = new long[len];
        mask = len - 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.s = -s;
        this.s1 = s + 1;
    }

    public void render(MatrixStack stack, VertexConsumer consumer) {
        for (int i = 0; i < points.length; i++) {
            stack.push();
            long pos = points[i];
            stack.translate(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos));
            WorldRenderer.drawBox(stack, consumer, s, s, s, s1, s1, s1, r, g, b, i + 1 == (index & mask) ? 0.75F : 0.25F * ((float) ((i - index) & mask) / points.length));
            stack.pop();
        }
    }

    public void push(long point) {
        points[index++ & mask] = point;
    }
}
