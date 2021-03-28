package net.kjp12.plymouth.anti_xray.mixins.world;

import net.kjp12.plymouth.anti_xray.Constants;
import net.kjp12.plymouth.anti_xray.IShadowChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.InfestedBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.BitSet;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements Chunk, IShadowChunk {
    @Shadow
    @Final
    private ChunkSection[] sections;
    @Shadow
    private BiomeArray biomeArray;
    @Shadow
    @Final
    private World world;
    @Shadow
    @Final
    private ChunkPos pos;
    @Unique
    private ChunkSection[] helium$shadowSections;
    // Used for block-entity fetching for the client.
    // May also be used to invalidate entities?
    // 16 separate sets instead of one massive set meant for saving memory when the corresponding section isn't present.
    @Unique
    private BitSet[] helium$shadowMasks;

    @Unique
    private static int toIndex(BlockPos pos) {
        return toIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    @Unique
    private static int toIndex(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    @Unique
    private static BitSet getOrCreateMask(BitSet[] masks, int y) {
        return masks[y] == null ? masks[y] = new BitSet(4096) : masks[y];
    }

    @Unique
    private static ChunkSection getOrCreateSection(ChunkSection[] sections, int y) {
        return sections[y] == null ? sections[y] = new ChunkSection(y * 16) : sections[y];
    }

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void helium$setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> bs, int i, int j, int k, ChunkSection chunkSection, boolean bl, BlockState old) {
        if (world instanceof ServerWorld && helium$shadowSections != null) {
            var index = toIndex(i, j, k);
            var mask = getOrCreateMask(helium$shadowMasks, j >> 4);
            if (state.isIn(Constants.HIDDEN_BLOCKS)) {
                if (mask.get(index) || (!old.isAir() && !old.isIn(Constants.NO_SMEAR_BLOCKS) && !old.isIn(Constants.HIDDEN_BLOCKS)))
                    // If the mask is already set or old is a valid candidate for smearing, we won't go ahead and (re)calculate the smear.
                    return;
                mask.set(index, true);
                if (state.getBlock() instanceof InfestedBlock) {
                    state = ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState();
                } else if (helium$isBlockHidden(state, pos.mutableCopy())) {
                    state = helium$getSmearBlock(pos, new BlockPos.Mutable());
                }
            } else {
                mask.set(index, false);
            }
            getOrCreateSection(helium$shadowSections, j >> 4).setBlockState(i & 15, j & 15, k & 15, state);
            ((ServerWorld) world).getChunkManager().markForUpdate(pos);
            if (VoxelShapes.matchesAnywhere(state.getCollisionShape(world, pos), old.getCollisionShape(world, pos), BooleanBiFunction.NOT_SAME)) {
                var bp = new BlockPos.Mutable();
                helium$setIfHidden(bp.set(pos, Direction.NORTH));
                helium$setIfHidden(bp.set(pos, Direction.SOUTH));
                helium$setIfHidden(bp.set(pos, Direction.EAST));
                helium$setIfHidden(bp.set(pos, Direction.WEST));
                helium$setIfHidden(bp.set(pos, Direction.UP));
                helium$setIfHidden(bp.set(pos, Direction.DOWN));
            }
        }
    }

    @Unique
    private BlockState getBlock(BlockPos bp) {
        int cx = bp.getX() >> 4, cz = bp.getZ() >> 4;
        if (cx == pos.x && cz == pos.z) return getBlockState(bp);
        var c = world.getChunk(cx, cz, ChunkStatus.LIGHT, false);
        return c == null ? Blocks.STONE.getDefaultState() : c.getBlockState(bp);
    }

    @Unique
    private void helium$setIfHidden(final BlockPos.Mutable bp) {
        // This is called by other chunks, shadowMask check is required.
        if (World.isOutOfBuildLimitVertically(bp) || helium$shadowMasks == null) return;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                cx = x >> 4, cy = y >> 4, cz = z >> 4;
        if (pos.x == cx && pos.z == cz) {
            var section = sections[cy];
            if (ChunkSection.isEmpty(section)) return;
            int ox = x & 15, oy = y & 15, oz = z & 15;
            var state = section.getBlockState(ox, oy, oz);
            if (!state.isAir() && state.isIn(Constants.HIDDEN_BLOCKS)) {
                var shadowMask = getOrCreateMask(helium$shadowMasks, cy);
                var f = state.getBlock() instanceof InfestedBlock;
                if (shadowMask.get(toIndex(ox, oy, oz))) {
                    if (!f && !helium$isBlockHidden(state, bp.set(ox, y, oz))) {
                        plymouth$unsetShadowBlock(bp.set(x, y, z));
                        // getOrCreateSection(helium$shadowSections, cy).setBlockState(ox, oy, oz, state);
                        // shadowMask.set(toIndex(ox, oy, oz), false);
                        // ((ServerWorld) world).getChunkManager().markForUpdate(bp.set(x, y, z));
                    }
                } else {
                    if (f || helium$isBlockHidden(state, bp.set(ox, y, oz))) {
                        plymouth$setShadowBlock(bp.set(x, y, z), f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : helium$getSmearBlock(bp.toImmutable(), bp));
                        // getOrCreateSection(helium$shadowSections, cy).setBlockState(ox, oy, oz, );
                        // shadowMask.set(toIndex(ox, oy, oz), true);
                        // ((ServerWorld) world).getChunkManager().markForUpdate(bp.set(x, y, z));
                    }
                }
            }
        } else {
            var chunk = world.getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk instanceof WorldChunk) //noinspection ConstantConditions
                ((MixinWorldChunk) chunk).helium$setIfHidden(bp);
        }
    }

    @Unique
    private boolean helium$isBlockHidden(final BlockState state,
                                         final BlockPos.Mutable bp) {
        // TODO: Account for light levels if Overworld.
        if (state.isAir()) return false;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                i = (pos.x << 4) | x, k = (pos.z << 4) | z;
        return plymouth$isCulling(getBlock(bp.set(i - 1, y, k)), Direction.EAST, bp)
                && plymouth$isCulling(getBlock(bp.set(i + 1, y, k)), Direction.WEST, bp)
                && plymouth$isCulling(getBlock(bp.set(i, y, k - 1)), Direction.SOUTH, bp)
                && plymouth$isCulling(getBlock(bp.set(i, y, k + 1)), Direction.NORTH, bp)
                && plymouth$isCulling(getBlockState(bp.set(x, y - 1, z)), Direction.UP, bp)
                && plymouth$isCulling(getBlockState(bp.set(x, y + 1, z)), Direction.DOWN, bp);
    }

    @Override
    public boolean plymouth$isCulling(final BlockState state,
                                      final Direction from,
                                      final BlockPos bp) {
        var fluid = state.getFluidState();
        return (fluid.isIn(FluidTags.LAVA) && from != Direction.UP && (from == Direction.DOWN || getBlock(bp.up()).getFluidState().isIn(FluidTags.LAVA))) ||
                (state.isOpaque() && Block.isFaceFullSquare(state.getCollisionShape(world, bp), from));
    }

    @Unique
    private void helium$generateShadow() {
        if (helium$shadowSections == null) helium$shadowSections = new ChunkSection[sections.length];
        if (helium$shadowMasks == null) helium$shadowMasks = new BitSet[sections.length];
        for (int i = 0; i < sections.length; i++) {
            helium$shadowSections[i] = sections[i] != null ? new ChunkSection(i * 16) : null;
            helium$shadowMasks[i] = null;
        }
        boolean f;
        var bp = new BlockPos.Mutable();
        for (int sy = 0; sy < 16; sy++) {
            var section = sections[sy];
            if (section == null) continue;
            var shadowSection = helium$shadowSections[sy];
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    var smear = biomeArray.getBiomeForNoiseGen(x >> 2, sy << 2, z >> 2).getGenerationSettings().getSurfaceConfig().getUnderMaterial();
                    for (int y = 0; y < 16; y++) {
                        var state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        if (state.isIn(Constants.HIDDEN_BLOCKS)) {
                            f = state.getBlock() instanceof InfestedBlock;
                            if (f || helium$isBlockHidden(state, bp.set(x, (sy << 4) | y, z))) {
                                shadowSection.setBlockState(x, y, z, f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : smear);
                                getOrCreateMask(helium$shadowMasks, sy).set(toIndex(x, y, z), true);
                            } else {
                                shadowSection.setBlockState(x, y, z, state);
                            }
                        } else {
                            shadowSection.setBlockState(x, y, z, state);
                            if (state.getFluidState().isEmpty() && !state.getBlock().hasBlockEntity() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) {
                                smear = state;
                            }
                        }
                    }
                }
        }
    }

    @Unique
    private BlockState helium$getSmearBlock(BlockPos pos, BlockPos.Mutable bp) {
        BlockState state;
        if ((!(state = plymouth$getShadowBlock(bp.set(pos, Direction.DOWN))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) ||
                (!(state = plymouth$getShadowBlock(bp.set(pos, Direction.UP))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) ||
                (!(state = plymouth$getShadowBlock(bp.set(pos, Direction.NORTH))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) ||
                (!(state = plymouth$getShadowBlock(bp.set(pos, Direction.SOUTH))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) ||
                (!(state = plymouth$getShadowBlock(bp.set(pos, Direction.EAST))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)) ||
                (!(state = plymouth$getShadowBlock(bp.set(pos, Direction.WEST))).isAir() && !state.isIn(Constants.NO_SMEAR_BLOCKS)))
            return state;
        Constants.LOGGER.error("Block is hidden yet smear failed to get a surrounding block?! {} -> {} @ {}", world, getBlockState(pos), pos);
        if (world.getRegistryKey().equals(World.OVERWORLD))
            return Blocks.STONE.getDefaultState();
        return biomeArray.getBiomeForNoiseGen(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).getGenerationSettings().getSurfaceConfig().getUnderMaterial();
    }

    @NotNull
    public BlockState plymouth$getShadowBlock(BlockPos pos) {
        if (helium$shadowSections == null) return Blocks.VOID_AIR.getDefaultState();
        int i = pos.getX(), j = pos.getY(), k = pos.getZ();
        try {
            if (j >= 0 && j >> 4 < helium$shadowSections.length) {
                var section = helium$shadowSections[j >> 4];
                return ChunkSection.isEmpty(section) ? Blocks.AIR.getDefaultState() : section.getBlockState(i & 15, j & 15, k & 15);
            }
            return Blocks.VOID_AIR.getDefaultState();
        } catch (Throwable t) {
            var report = CrashReport.create(t, "Helium: Getting shadow block state");
            report.addElement("Block being got")
                    .add("Location", () -> CrashReportSection.createPositionString(i, j, k))
                    .add("Section", () -> "Section " + (j >> 4) + ": " + (helium$shadowSections == null ? "shadowSections -> null?" : helium$shadowSections[j >> 4]));
            throw new CrashException(report);
        }
    }

    public void plymouth$unsetShadowBlock(BlockPos pos) {
        if (helium$shadowMasks == null) return; // There is nothing to unset.
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ(), cy = y >> 4, i = toIndex(pos);
        final var m = getOrCreateMask(helium$shadowMasks, cy);
        if (m.get(i)) {
            getOrCreateSection(helium$shadowSections, cy).setBlockState(x & 15, y & 15, z & 15,
                    getOrCreateSection(sections, cy).getBlockState(x & 15, y & 15, z & 15));
            m.set(i, false);
            ((ServerWorld) world).getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    public void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        final int y = pos.getY(), cy = y >> 4;
        // Do note, we're calling helium$getShadowSections() to force generation of the shadow chunk.
        getOrCreateSection(plymouth$getShadowSections(), cy).setBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15, state);
        getOrCreateMask(helium$shadowMasks, cy).set(toIndex(pos), true);
        ((ServerWorld) world).getChunkManager().markForUpdate(pos);
    }

    @Override
    @NotNull
    public ChunkSection[] plymouth$getShadowSections() {
        if (!ArrayUtils.isEmpty(sections) && ArrayUtils.isEmpty(helium$shadowSections))
            helium$generateShadow();
        return helium$shadowSections;
    }

    @Override
    public boolean plymouth$isMasked(BlockPos pos) {
        if (helium$shadowMasks == null) return false;
        var mask = helium$shadowMasks[pos.getY() >> 4];
        return mask != null && mask.get(toIndex(pos));
    }
}
