/*
 * SPDX-FileCopyRightText: 2021 ishland <https://github.com/ishland>, irtimaled <https://github.com/irtimaled>
 * SPDX-License-Identifier: MIT
 *
 * This file is licensed under the MIT license.
 * The originating source can be found at https://github.com/irtimaled/BoundingBoxOutlineReloaded/blob/bca7c1e95d2b17051dc60788234d00a0ed0542fb/src/main/java/com/irtimaled/bbor/client/renderers/RenderBatch.java
 * */
package net.kjp12.plymouth.debug;// Created 2022-05-01T09:10:05

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Batching system for lines and boxes.
 *
 * @author ishland
 * @author irtimaled
 * @see <a href=https://github.com/irtimaled/BoundingBoxOutlineReloaded/blob/1.17.1-fabric-dev/src/main/java/com/irtimaled/bbor/client/renderers/RenderBatch.java>RenderBatch from BoundingBoxOutlineReloaded on GitHub.</a>
 * @since ${version}
 */
public class RenderBatch {

    private static final BufferBuilder quadBufferBuilderNonMasked = new BufferBuilder(2097152);
    private static final BufferBuilder quadBufferBuilderMasked = new BufferBuilder(2097152);
    private static final BufferBuilder lineBufferBuilderMasked = new BufferBuilder(2097152);
    private static final BufferBuilder lineBufferBuilderNonMasked = new BufferBuilder(2097152);

    private static final Object mutex = new Object();
    private static final AtomicLong quadNonMaskedCount = new AtomicLong(0L);
    private static final AtomicLong quadMaskedCount = new AtomicLong(0L);
    private static final AtomicLong lineMaskedCount = new AtomicLong(0L);
    private static final AtomicLong lineNonMaskedCount = new AtomicLong(0L);

    private static final AtomicLong quadNonMaskedCountLast = new AtomicLong(0L);
    private static final AtomicLong quadMaskedCountLast = new AtomicLong(0L);
    private static final AtomicLong lineCountLast = new AtomicLong(0L);
    private static final AtomicLong lineNonMaskedCountLast = new AtomicLong(0L);

    private static final AtomicLong lastDurationNanos = new AtomicLong(0L);

    public static void beginBatch() {
        quadBufferBuilderMasked.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quadBufferBuilderNonMasked.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        lineBufferBuilderMasked.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        lineBufferBuilderNonMasked.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }

    /**
     * Draws a solid box in the world.
     * <p>
     * Note: This method was changed from the original by expanding
     * {@link Box Box} and {@link java.awt.Color Color} to their base components.
     *
     * @param matrixEntry The matrix entry used as a reference.
     * @param minX        Starting X position
     * @param minY        Starting Y position
     * @param minZ        Starting Z position
     * @param maxX        Ending X position
     * @param maxY        Ending Y position
     * @param maxZ        Ending Z position
     * @param red         Redness of the box.
     * @param green       Greenness of the box.
     * @param blue        Blueness of the box.
     * @param alpha       How solid is the box?
     * @param mask        Is the depth mask used?
     * @param sameX       If to not draw X planes. ?
     * @param sameY       If to not draw Y planes. ?
     * @param sameZ       If to not draw Z planes. ?
     */
    public static void drawSolidBox(MatrixStack.Entry matrixEntry,
                                    final float minX, final float minY, final float minZ,
                                    final float maxX, final float maxY, final float maxZ,
                                    final int red, final int green, final int blue, final int alpha,
                                    final boolean mask, final boolean sameX, final boolean sameY, final boolean sameZ) {
        final BufferBuilder bufferBuilder = mask ? RenderBatch.quadBufferBuilderMasked : RenderBatch.quadBufferBuilderNonMasked;
        final AtomicLong quadCount = mask ? quadMaskedCount : quadNonMaskedCount;

        if (!sameX && !sameZ) {
            quadCount.getAndIncrement();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
            if (!sameY) {
                quadCount.getAndIncrement();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
            }
        }

        if (!sameX && !sameY) {
            quadCount.getAndIncrement();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
            if (!sameZ) {
                quadCount.getAndIncrement();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
            }
        }

        if (!sameY && !sameZ) {
            quadCount.getAndIncrement();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
            if (!sameX) {
                quadCount.getAndIncrement();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();
            }
        }
    }

    public static void drawWireBox(MatrixStack.Entry matrixEntry,
                                   final float minX, final float minY, final float minZ,
                                   final float maxX, final float maxY, final float maxZ,
                                   final int red, final int green, final int blue, final int alpha,
                                   final boolean mask) {
        final BufferBuilder bufferBuilder = mask ? RenderBatch.lineBufferBuilderMasked : RenderBatch.lineBufferBuilderNonMasked;
        (mask ? RenderBatch.lineMaskedCount : lineNonMaskedCount).getAndAdd(16);

        // X, lY
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();

        // X, uY
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();

        // Z, lY
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();

        // Z, uY
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();

        // Y
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), minX, maxY, maxZ).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, minZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, minY, maxZ).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrixEntry.getPositionMatrix(), maxX, maxY, maxZ).color(red, green, blue, alpha).next();
    }

    /**
     * Draws a line in the world.
     * <p>
     * Note: This method was changed from the original by expanding
     * {@link java.awt.Point Point} and {@link java.awt.Color Color}
     * to their base components, while replacing the Camera reference with a parameter pass through.
     *
     * Camera X/Z was also removed in favour of transforming on input.
     *
     * @param matrixEntry The matrix entry used as a reference.
     * @param startX      Starting X position
     * @param startY      Starting Y position
     * @param startZ      Starting Z position
     * @param endX        Ending X position
     * @param endY        Ending Y position
     * @param endZ        Ending Z position
     * @param red         Redness of the line
     * @param green       Greenness of the line
     * @param blue        Blueness of the line
     * @param alpha       How solid is the line?
     * @param mask        Is the depth mask used?
     */
    public static void drawLine(MatrixStack.Entry matrixEntry,
                                final float startX, final float startY, final float startZ,
                                final float endX, final float endY, final float endZ,
                                final int red, final int green, final int blue, final int alpha,
                                final boolean mask) {
        final var bufferBuilder = mask ? lineBufferBuilderMasked : lineBufferBuilderNonMasked;
        (mask ? lineMaskedCount : lineNonMaskedCount).getAndIncrement();

        bufferBuilder
                .vertex(matrixEntry.getPositionMatrix(),
                        startX,
                        startY,
                        startZ)
                .color(red, green, blue, alpha)
                .next();
        bufferBuilder
                .vertex(matrixEntry.getPositionMatrix(),
                        endX,
                        endY,
                        endZ)
                .color(red, green, blue, alpha)
                .next();
    }

    public static void endBatch() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        long startTime = System.nanoTime();
        var quadBufferMasked = quadBufferBuilderMasked.end();
        var quadBufferNonMasked = quadBufferBuilderNonMasked.end();
        var lineBufferMasked = lineBufferBuilderMasked.end();
        var lineBufferNonMasked = lineBufferBuilderNonMasked.end();

        synchronized (mutex) {
            quadMaskedCountLast.set(quadMaskedCount.getAndSet(0L));
            quadNonMaskedCountLast.set(quadNonMaskedCount.getAndSet(0L));
            lineCountLast.set(lineMaskedCount.getAndSet(0L));
            lineNonMaskedCountLast.set(lineNonMaskedCount.getAndSet(0L));
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableTexture();
        RenderSystem.lineWidth(2.5F);
        RenderSystem.enableBlend();

        BufferRenderer.drawWithShader(quadBufferMasked);
        BufferRenderer.drawWithShader(lineBufferMasked);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferRenderer.drawWithShader(quadBufferNonMasked);
        BufferRenderer.drawWithShader(lineBufferNonMasked);

        lastDurationNanos.set(System.nanoTime() - startTime);

        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public static String debugString() {
        return String.format("[BBOR] Statistics: Filled faces: %d,%d Lines: %d @ %.2fms", quadMaskedCountLast.get(), quadNonMaskedCountLast.get(), lineCountLast.get(), lastDurationNanos.get() / 1_000_000.0);
    }

}