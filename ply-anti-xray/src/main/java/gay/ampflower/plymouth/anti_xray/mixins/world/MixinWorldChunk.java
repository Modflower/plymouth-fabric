package gay.ampflower.plymouth.anti_xray.mixins.world;

import gay.ampflower.plymouth.anti_xray.Constants;
import gay.ampflower.plymouth.anti_xray.IShadowChunk;
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
    private ChunkSection[] shadowSections;
    // Used for block-entity fetching for the client.
    // May also be used to invalidate entities?
    // 16 separate sets instead of one massive set meant for saving memory when the corresponding section isn't present.
    @Unique
    private BitSet[] shadowMasks;

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void plymouth$setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> bs, int i, int j, int k, ChunkSection chunkSection, boolean bl, BlockState old) {
        if (world instanceof ServerWorld && shadowSections != null) {
            var index = Constants.toIndex(i, j, k);
            var mask = Constants.getOrCreateMask(shadowMasks, j >> 4);
            if (state.isIn(Constants.HIDDEN_BLOCKS)) {
                if (mask.get(index)) return;
                var mutPos = pos.mutableCopy();
                if (state.getBlock() instanceof InfestedBlock) {
                    var regular = ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState();
                    if (regular.isIn(Constants.HIDDEN_BLOCKS) && plymouth$isBlockHidden(regular, mutPos)) {
                        if (old != regular && Constants.isHidingCandidate(old)) return;
                        state = getSmearBlock(mutPos.set(pos));
                    } else {
                        state = regular;
                    }
                    mask.set(index, true);
                } else if (plymouth$isBlockHidden(state, mutPos)) {
                    if (Constants.isHidingCandidate(old)) return;
                    state = getSmearBlock(mutPos.set(pos));
                    mask.set(index, true);
                }
            } else {
                mask.set(index, false);
            }
            Constants.getOrCreateSection(shadowSections, j >> 4).setBlockState(i & 15, j & 15, k & 15, state);
            plymouth$trackUpdate(pos);
            if (VoxelShapes.matchesAnywhere(state.getCollisionShape(world, pos), old.getCollisionShape(world, pos), BooleanBiFunction.NOT_SAME)) {
                var bp = new BlockPos.Mutable();
                setIfHidden(bp.set(pos, Direction.NORTH));
                setIfHidden(bp.set(pos, Direction.SOUTH));
                setIfHidden(bp.set(pos, Direction.EAST));
                setIfHidden(bp.set(pos, Direction.WEST));
                setIfHidden(bp.set(pos, Direction.UP));
                setIfHidden(bp.set(pos, Direction.DOWN));
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
    private void setIfHidden(final BlockPos.Mutable bp) {
        // This is called by other chunks, shadowMask check is required.
        if (World.isOutOfBuildLimitVertically(bp) || shadowMasks == null) return;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                cx = x >> 4, cy = y >> 4, cz = z >> 4;
        if (pos.x == cx && pos.z == cz) {
            var section = sections[cy];
            if (ChunkSection.isEmpty(section)) return;
            int ox = x & 15, oy = y & 15, oz = z & 15;
            var state = section.getBlockState(ox, oy, oz);
            if (!state.isAir() && state.isIn(Constants.HIDDEN_BLOCKS)) {
                var shadowMask = Constants.getOrCreateMask(shadowMasks, cy);
                var f = state.getBlock() instanceof InfestedBlock;
                if (shadowMask.get(Constants.toIndex(ox, oy, oz))) {
                    if (!f && !plymouth$isBlockHidden(state, bp.set(ox, y, oz))) {
                        plymouth$unsetShadowBlock(bp.set(x, y, z));
                    }
                } else {
                    if (f || plymouth$isBlockHidden(state, bp.set(ox, y, oz))) {
                        var smear = f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : getSmearBlock(bp.set(ox, y, oz));
                        plymouth$setShadowBlock(bp.set(x, y, z), smear);
                    }
                }
            }
        } else {
            var chunk = world.getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk instanceof WorldChunk) //noinspection ConstantConditions
                ((MixinWorldChunk) chunk).setIfHidden(bp);
        }
    }

    public boolean plymouth$isBlockHidden(final BlockState state,
                                          final BlockPos.Mutable bp) {
        if (state.isAir()) return false;
        // TODO: Account for light levels if Overworld.
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                i = (pos.x << 4) | x, k = (pos.z << 4) | z;
        IShadowChunk.super.plymouth$isBlockHidden(state, bp.set(i, y, k));
        return plymouth$isCulling(getBlockState(bp.set(x, y - 1, z)), Direction.UP, bp)
                && plymouth$isCulling(getBlockState(bp.set(x, y + 1, z)), Direction.DOWN, bp)
                && plymouth$isCulling(getBlock(bp.set(i - 1, y, k)), Direction.EAST, bp)
                && plymouth$isCulling(getBlock(bp.set(i + 1, y, k)), Direction.WEST, bp)
                && plymouth$isCulling(getBlock(bp.set(i, y, k - 1)), Direction.SOUTH, bp)
                && plymouth$isCulling(getBlock(bp.set(i, y, k + 1)), Direction.NORTH, bp);
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
    private void generateShadow() {
        if (shadowSections == null) shadowSections = new ChunkSection[sections.length];
        if (shadowMasks == null) shadowMasks = new BitSet[sections.length];
        for (int i = 0; i < sections.length; i++) {
            shadowSections[i] = sections[i] != null ? new ChunkSection(i * 16) : null;
            shadowMasks[i] = null;
        }
        boolean f;
        var bp = new BlockPos.Mutable();
        for (int sy = 0; sy < 16; sy++) {
            var section = sections[sy];
            if (section == null) continue;
            var shadowSection = shadowSections[sy];
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    var smear = biomeArray.getBiomeForNoiseGen(x >> 2, sy << 2, z >> 2).getGenerationSettings().getSurfaceConfig().getUnderMaterial();
                    for (int y = 0; y < 16; y++) {
                        var state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        if (state.isIn(Constants.HIDDEN_BLOCKS)) {
                            if (state.getBlock() instanceof InfestedBlock) {
                                state = ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState();
                                Constants.getOrCreateMask(shadowMasks, sy).set(Constants.toIndex(x, y, z), true);
                            } else if (plymouth$isBlockHidden(state, bp.set(x, (sy << 4) | y, z))) {
                                state = smear;
                                Constants.getOrCreateMask(shadowMasks, sy).set(Constants.toIndex(x, y, z), true);
                            }
                        } else if (Constants.isHidingCandidate(state)) {
                            smear = state;
                        }
                        shadowSection.setBlockState(x, y, z, state);
                    }
                }
        }
    }

    @Unique
    private BlockState getSmearBlock(BlockPos.Mutable bp) {
        int x = bp.getX(), y = bp.getY(), z = bp.getZ();
        BlockState state;
        if (Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y - 1, z))) ||
                Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y + 1, z))) ||
                Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z - 1))) ||
                Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z + 1))) ||
                Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x + 1, y, z))) ||
                Constants.isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x - 1, y, z))))
            return state;
        bp.set(x, y, z);
        Constants.LOGGER.error("Block is hidden yet smear failed to get a surrounding block?! {} -> {} @ {}", world, getBlockState(bp), bp, new Throwable());
        return getDefaultSmearBlock(bp);
    }

    @Unique
    private BlockState getDefaultSmearBlock(BlockPos pos) {
        return world.getRegistryKey().equals(World.OVERWORLD) ? Blocks.STONE.getDefaultState() : biomeArray.getBiomeForNoiseGen(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).getGenerationSettings().getSurfaceConfig().getUnderMaterial();
    }

    @NotNull
    public BlockState plymouth$getShadowBlock(BlockPos pos) {
        if (shadowSections == null) return Blocks.VOID_AIR.getDefaultState();
        int i = pos.getX(), j = pos.getY(), k = pos.getZ();
        try {
            if (j >= 0 && j >> 4 < shadowSections.length) {
                var section = shadowSections[j >> 4];
                return ChunkSection.isEmpty(section) ? Blocks.AIR.getDefaultState() : section.getBlockState(i & 15, j & 15, k & 15);
            }
            return Blocks.VOID_AIR.getDefaultState();
        } catch (Throwable t) {
            var report = CrashReport.create(t, "Plymouth: Anti-Xray: Getting shadow block state");
            report.addElement("Block being got")
                    .add("Location", () -> CrashReportSection.createPositionString(i, j, k))
                    .add("Section", () -> "Section " + (j >> 4) + ": " + (shadowSections == null ? "shadowSections -> null?" : shadowSections[j >> 4]));
            throw new CrashException(report);
        }
    }

    @Override
    public void plymouth$unsetShadowBlock(BlockPos pos) {
        if (shadowMasks == null) return; // There is nothing to unset.
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ(), cy = y >> 4, i = Constants.toIndex(pos);
        final var m = Constants.getOrCreateMask(shadowMasks, cy);
        if (m.get(i)) {
            Constants.getOrCreateSection(shadowSections, cy).setBlockState(x & 15, y & 15, z & 15,
                    Constants.getOrCreateSection(sections, cy).getBlockState(x & 15, y & 15, z & 15));
            m.set(i, false);
            plymouth$trackUpdate(pos);
        }
    }

    @Override
    public void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        IShadowChunk.super.plymouth$setShadowBlock(pos, state);
        final int y = pos.getY(), cy = y >> 4;
        // Do note, we're calling plymouth$getShadowSections() to force generation of the shadow chunk.
        Constants.getOrCreateSection(plymouth$getShadowSections(), cy).setBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15, state);
        Constants.getOrCreateMask(shadowMasks, cy).set(Constants.toIndex(pos), true);
        plymouth$trackUpdate(pos);
    }

    @Override
    @NotNull
    public ChunkSection[] plymouth$getShadowSections() {
        if (!ArrayUtils.isEmpty(sections) && ArrayUtils.isEmpty(shadowSections))
            generateShadow();
        return shadowSections;
    }

    @Override
    public BitSet[] plymouth$getShadowMasks() {
        return shadowMasks;
    }

    @Override
    public boolean plymouth$isMasked(BlockPos pos) {
        if (shadowMasks == null) return false;
        var mask = shadowMasks[pos.getY() >> 4];
        return mask != null && mask.get(Constants.toIndex(pos));
    }

    @Override
    public void plymouth$trackUpdate(BlockPos pos) {
        IShadowChunk.super.plymouth$trackUpdate(pos);
        ((ServerWorld) world).getChunkManager().markForUpdate(pos);
    }
}
