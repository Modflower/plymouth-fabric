package gay.ampflower.plymouth.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

/**
 * @author Ampflower
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugProfiler {
    private final long[] points;
    private final float r, g, b, m, m1, s;
    private final int mask;
    private int index;

    public DebugProfiler(int length, float r, float g, float b, float m, float s) {
        int len = 1 << MathHelper.log2DeBruijn(length);
        this.points = new long[len];
        mask = len - 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.m = -m;
        this.m1 = m + 1;
        this.s = s;
    }

    public DebugProfiler(int length, float r, float g, float b, float m) {
        this(length, r, g, b, m, 1F);
    }

    public void render(MatrixStack stack, VertexConsumer consumer) {
        stack.push();
        stack.scale(s, s, s);
        for (int i = 0; i < points.length; i++) {
            stack.push();
            long pos = points[i];
            stack.translate(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos));
            WorldRenderer.drawBox(stack, consumer, m, m, m, m1, m1, m1, r, g, b, i + 1 == (index & mask) ? 0.75F : 0.25F * ((float) ((i - index) & mask) / points.length));
            stack.pop();
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
