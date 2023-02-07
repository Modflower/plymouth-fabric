package net.kjp12.plymouth.debug.misc;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.kjp12.plymouth.debug.RenderBatch;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.lang3.function.TriFunction;

import static net.kjp12.plymouth.debug.Fusebox.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class BoundingBoxDebugClient {
    public static void initialise() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BoundingBoxDebugClient::render);
    }

    private static void render(WorldRenderContext ctx) {
        // Note: We purposely don't enable a depth test for the sake of visibility.
        // However, due to which stage it's on, it does get obstructed by water and clouds.
        RenderBatch.beginBatch();

        // We don't use ctx.matrixStack() as it's far easier to just create a new
        // MatrixStack and set it accordingly for what we need.
        var matrices = new MatrixStack();
        var camera = ctx.camera().getPos();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (viewCollision) {
            renderBounding(ctx, matrices, viewCollisionRange, BlockState::getCollisionShape, viewCollisionMask, viewCollisionWire);
        }

        RenderBatch.endBatch();
    }

    private static void renderBounding(final WorldRenderContext ctx, final MatrixStack matrices, final int range,
                                       final TriFunction<BlockState, ClientWorld, BlockPos, VoxelShape> shapeExtractor,
                                       final boolean mask, final boolean wire) {
        final ClientWorld world = ctx.world();
        final BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        final BlockPos.Mutable pos = new BlockPos.Mutable();
        final int x = playerPos.getX(), y = playerPos.getY(), z = playerPos.getZ();

        final VoxelShapes.BoxConsumer consumer = wire ? wireEmitter(matrices, pos, mask) : boxEmitter(matrices, pos, mask);

        for (int ix = x - range, ixl = x + range; ix <= ixl; ix++) {
            for (int iz = z - range, izl = z + range; iz <= izl; iz++) {
                for (int iy = y - range, iyl = y + range; iy <= iyl; iy++) {
                    final BlockState state = world.getBlockState(pos.set(ix, iy, iz));

                    // Skip air, guaranteed empty
                    if (state.isAir()) continue;

                    final VoxelShape shape = shapeExtractor.apply(state, world, pos);

                    // Skip anything with a full shape to avoid visual noise.
                    if (shape == null || shape.isEmpty() || shape == VoxelShapes.fullCube()) continue;

                    if (wire) {
                        shape.forEachEdge(consumer);
                    } else {
                        shape.forEachBox(consumer);
                    }
                }
            }
        }
    }

    private static VoxelShapes.BoxConsumer wireEmitter(final MatrixStack matrices, final BlockPos.Mutable pos,
                                                       final boolean mask) {
        return (x1, y1, z1, x2, y2, z2) -> {
            final int bx = pos.getX(), by = pos.getY(), bz = pos.getZ();
            final float fx1 = (float) (x1 + bx), fy1 = (float) (y1 + by), fz1 = (float) (z1 + bz),
                    fx2 = (float) (x2 + bx), fy2 = (float) (y2 + by), fz2 = (float) (z2 + bz);
            RenderBatch.drawLine(matrices.peek(),
                    fx1, fy1, fz1,
                    fx2, fy2, fz2,
                    255, 255, 255, 255,
                    mask);
        };
    }

    private static VoxelShapes.BoxConsumer boxEmitter(final MatrixStack matrices, final BlockPos.Mutable pos,
                                                      final boolean mask) {
        return (x1, y1, z1, x2, y2, z2) -> {
            final int bx = pos.getX(), by = pos.getY(), bz = pos.getZ();
            final float fx1 = (float) (x1 + bx), fy1 = (float) (y1 + by), fz1 = (float) (z1 + bz),
                    fx2 = (float) (x2 + bx), fy2 = (float) (y2 + by), fz2 = (float) (z2 + bz);
            RenderBatch.drawSolidBox(matrices.peek(),
                    fx1, fy1, fz1,
                    fx2, fy2, fz2,
                    255, 255, 255, 255,
                    mask, false, false, false);
        };
    }
}
