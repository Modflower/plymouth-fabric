package gay.ampflower.plymouth.debug.misc;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import gay.ampflower.plymouth.debug.RenderBatch;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.CollisionView;
import org.apache.commons.lang3.function.TriFunction;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static gay.ampflower.plymouth.debug.Fusebox.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class BoundingBoxDebugClient {
    private static Optional<Constructor<BlockCollisionSpliterator>> collisionSpliteratorConstructor;
    private static boolean collisionSpliteratorConstructorFaulted;

    public static void initialise() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BoundingBoxDebugClient::render);
        ReloadDebugClient.addReloadable(() -> {
            collisionSpliteratorConstructor = null;
            collisionSpliteratorConstructorFaulted = false;
        });
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
            if (collisionSpliteratorAvailable()) {
                renderBounding(ctx, matrices, viewCollisionRange, viewCollisionMask, viewCollisionWire);
            } else {
                renderBounding(ctx, matrices, viewCollisionRange, viewCollisionMask, viewCollisionWire, BlockState::getCollisionShape);
            }
        }

        RenderBatch.endBatch();
    }

    private static void renderBounding(final WorldRenderContext ctx, final MatrixStack matrices,
                                       final int range, final boolean mask, final boolean wire) {
        final ClientWorld world = ctx.world();
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        final BlockPos.Mutable pos = new BlockPos.Mutable();

        final VoxelShapes.BoxConsumer consumer = wire ? wireEmitter(matrices, pos, mask) : boxEmitter(matrices, pos, mask);

        final var itr = new BlockCollisionSpliterator(world, null, boxFromRange(player.getPos(), range));

        while (itr.hasNext()) {
            final VoxelShape shape = itr.next();

            if (shape == null || shape.isEmpty() || shape == VoxelShapes.fullCube()) continue;

            if (wire) {
                shape.forEachEdge(consumer);
            } else {
                shape.forEachBox(consumer);
            }
        }
    }

    private static void renderBounding(final WorldRenderContext ctx, final MatrixStack matrices,
                                       final int range, final boolean mask, final boolean wire,
                                       final TriFunction<BlockState, ClientWorld, BlockPos, VoxelShape> shapeExtractor) {
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

    private static Box boxFromRange(final Vec3d center, final int range) {
        return new Box(center.x - range, center.y - range, center.z - range,
                center.x + range, center.y + range, center.z + range);
    }

    private static boolean collisionSpliteratorAvailable() {
        if (collisionSpliteratorConstructor == null) {
            if (viewCollisionClass != null && !viewCollisionClass.isBlank()) try {
                final var viewCollisionInst = BoundingBoxDebugClient.class.getClassLoader().loadClass(viewCollisionClass);

                if (BlockCollisionSpliterator.class.isAssignableFrom(viewCollisionInst)) {
                    collisionSpliteratorConstructor = Optional.of((Constructor<BlockCollisionSpliterator>)
                            viewCollisionInst.getConstructor(CollisionView.class, Entity.class, Box.class));
                    return true;
                }
            } catch (ReflectiveOperationException roe) {
                roe.printStackTrace();
            }
            collisionSpliteratorConstructor = Optional.empty();
        }
        return collisionSpliteratorConstructor.isPresent();
    }

    private static BlockCollisionSpliterator construct(CollisionView view, Box box) {
        try {
            return collisionSpliteratorConstructor.get().newInstance(view, null, box);
        } catch (ReflectiveOperationException roe) {
            if (!collisionSpliteratorConstructorFaulted) {
                roe.printStackTrace();
                collisionSpliteratorConstructorFaulted = true;
            }
        }
        return null;
    }
}
