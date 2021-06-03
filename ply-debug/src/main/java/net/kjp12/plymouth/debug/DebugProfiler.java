package net.kjp12.plymouth.debug;// Created 2021-03-29T00:14:15

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * @author KJP12
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugProfiler {
    private final long[] points;
    private final float r, g, b, s;
    private final int mask;
    private int index;

    public DebugProfiler(int length, float r, float g, float b, float s) {
        int len = 1 << MathHelper.log2DeBruijn(length);
        this.points = new long[len];
        mask = len - 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.s = s;
    }

    public void render(WorldRenderContext ctx) {
        // Note: We purposely don't enable a depth test for the sake of visibility.
        // However, due to which stage it's on, it does get obstructed by water and clouds.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();

        var tessellator = Tessellator.getInstance();
        var immediate = tessellator.getBuffer();

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.lineWidth(0.5F);

        immediate.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        var camera = ctx.camera().getPos();
        for (int i = 0; i < points.length; i++) {
            long pos = points[i];
            double x = BlockPos.unpackLongX(pos) - camera.x;
            double y = BlockPos.unpackLongY(pos) - camera.y;
            double z = BlockPos.unpackLongZ(pos) - camera.z;
            WorldRenderer.drawBox(immediate, x - s, y - s, z - s, x + 1 + s, y + 1 + s, z + 1 + s, r, g, b, i + 1 == (index & mask) ? 0.75F : 0.25F * ((float) ((i - index) & mask) / points.length));
        }
        tessellator.draw();

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
    }

    public void push(long point) {
        points[index++ & mask] = point;
    }
}
