package net.kjp12.sodium.mixins.world;

import net.kjp12.sodium.Main;
import net.kjp12.sodium.SodiumHelper;
import net.kjp12.sodium.helpers.IShadowChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.InfestedBlock;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.BitSet;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements Chunk, IShadowChunk {
    @Shadow
    @Final
    private static Logger LOGGER;
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
    private ChunkSection[] sodium$shadowSections;
    // Used for tile-entity fetching for the client.
    // May also be used to invalidate entities?
    // 16 separate sets instead of one massive set meant for saving memory when the corresponding section isn't present.
    private BitSet[] sodium$shadowMasks;

    private static int toIndex(BlockPos pos) {
        return toIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    private static int toIndex(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    private static BlockPos.Mutable getFromIndex(BlockPos.Mutable pos, int index) {
        return pos.set(index & 15, (index >> 8) & 15, (index >> 4) & 15);
    }

    private static BitSet getOrCreateMask(BitSet[] masks, int y) {
        return masks[y] == null ? masks[y] = new BitSet(4096) : masks[y];
    }

    private static ChunkSection getOrCreateSection(ChunkSection[] sections, int y) {
        return sections[y] == null ? sections[y] = new ChunkSection(y * 16) : sections[y];
    }

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract void addBlockEntity(BlockEntity blockEntity);

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void sodium$setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> bs, int i, int j, int k, ChunkSection chunkSection, boolean bl, BlockState old) {
        if (world instanceof ServerWorld && sodium$shadowSections != null) {
            var index = toIndex(i, j, k);
            var mask = getOrCreateMask(sodium$shadowMasks, j >> 4);
            if (state.isIn(SodiumHelper.HIDDEN_BLOCKS)) {
                if (mask.get(index)) return;
                var f = state.getBlock() instanceof InfestedBlock;
                if (f || sodium$isBlockHidden(state, pos.mutableCopy())) {
                    mask.set(index, true);
                    getOrCreateSection(sodium$shadowSections, j >> 4).setBlockState(i & 15, j & 15, k & 15,
                            f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : sodium$getSmearBlock(pos, new BlockPos.Mutable()));
                    ((ServerWorld) world).getChunkManager().markForUpdate(pos);
                } else {
                    getOrCreateSection(sodium$shadowSections, j >> 4).setBlockState(i & 15, j & 15, k & 15, state);
                }
            } else {
                mask.set(index, false);
                getOrCreateSection(sodium$shadowSections, j >> 4).setBlockState(i & 15, j & 15, k & 15, state);
            }
            if (VoxelShapes.matchesAnywhere(state.getCollisionShape(world, pos), old.getCollisionShape(world, pos), BooleanBiFunction.NOT_SAME)) {
                var bp = new BlockPos.Mutable();
                sodium$setIfHidden(bp.set(pos, Direction.NORTH));
                sodium$setIfHidden(bp.set(pos, Direction.SOUTH));
                sodium$setIfHidden(bp.set(pos, Direction.EAST));
                sodium$setIfHidden(bp.set(pos, Direction.WEST));
                sodium$setIfHidden(bp.set(pos, Direction.UP));
                sodium$setIfHidden(bp.set(pos, Direction.DOWN));
            }
        }
    }

    private BlockState getBlock(BlockPos bp) {
        int cx = bp.getX() >> 4, cz = bp.getZ() >> 4;
        if (cx == pos.x && cz == pos.z) return getBlockState(bp);
        var c = world.getChunk(cx, cz, ChunkStatus.LIGHT, false);
        return c == null ? Blocks.STONE.getDefaultState() : c.getBlockState(bp);
    }

    private void sodium$setIfHidden(final BlockPos.Mutable bp) {
        if (World.isHeightInvalid(bp)) return;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                cx = x >> 4, cy = y >> 4, cz = z >> 4;
        if (pos.x == cx && pos.z == cz) {
            var section = sections[cy];
            if (ChunkSection.isEmpty(section)) return;
            int ox = x & 15, oy = y & 15, oz = z & 15;
            var state = section.getBlockState(ox, oy, oz);
            if (!state.isAir() && state.isIn(SodiumHelper.HIDDEN_BLOCKS)) {
                var shadowMask = getOrCreateMask(sodium$shadowMasks, cy);
                var f = state.getBlock() instanceof InfestedBlock;
                if (shadowMask.get(toIndex(ox, oy, oz))) {
                    if (!f && !sodium$isBlockHidden(state, bp.set(ox, y, oz))) {
                        getOrCreateSection(sodium$shadowSections, cy).setBlockState(ox, oy, oz, state);
                        shadowMask.set(toIndex(ox, oy, oz), false);
                        ((ServerWorld) world).getChunkManager().markForUpdate(bp.set(x, y, z));
                    }
                } else {
                    if (f || sodium$isBlockHidden(state, bp.set(ox, y, oz))) {
                        var shadowSection = getOrCreateSection(sodium$shadowSections, cy);
                        shadowSection.setBlockState(ox, oy, oz, f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : sodium$getSmearBlock(new BlockPos(x, y, z), bp));
                        shadowMask.set(toIndex(ox, oy, oz), true);
                        ((ServerWorld) world).getChunkManager().markForUpdate(bp.set(x, y, z));
                    }
                }
            }
        } else {
            var chunk = world.getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk instanceof WorldChunk) //noinspection ConstantConditions
                ((MixinWorldChunk) chunk).sodium$setIfHidden(bp);
        }
    }

    private void sodium$setIfHidden(BlockPos.Mutable bp, int i) {
        // This method is called by external chunks, this method is required.
        if (sodium$shadowSections == null) return;
        getOrCreateMask(sodium$shadowMasks, i).set(toIndex(bp), sodium$isBlockHidden(getBlockState(bp), bp));
    }

    private boolean sodium$isBlockHidden(final BlockState state,
                                         final BlockPos.Mutable bp) {
        // TODO: Account for light levels if Overworld.
        if (state.isAir()) return false;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                i = (pos.x << 4) | x, k = (pos.z << 4) | z;
        return sodium$isCulling(getBlock(bp.set(i - 1, y, k)), Direction.EAST, bp)
                && sodium$isCulling(getBlock(bp.set(i + 1, y, k)), Direction.WEST, bp)
                && sodium$isCulling(getBlock(bp.set(i, y, k - 1)), Direction.SOUTH, bp)
                && sodium$isCulling(getBlock(bp.set(i, y, k + 1)), Direction.NORTH, bp)
                && sodium$isCulling(getBlockState(bp.set(x, y - 1, z)), Direction.UP, bp)
                && sodium$isCulling(getBlockState(bp.set(x, y + 1, z)), Direction.DOWN, bp);
    }

    private boolean sodium$isCulling(final BlockState state,
                                     final Direction from,
                                     final BlockPos bp) {
        var fluid = state.getFluidState();
        return (fluid.matches(FluidTags.LAVA) && from != Direction.UP && (from == Direction.DOWN || getBlock(bp.up()).getFluidState().matches(FluidTags.LAVA))) ||
                (state.isOpaque() && Block.isFaceFullSquare(state.getCollisionShape(world, bp), from));
    }

    private void sodium$generateShadow() {
        if (sodium$shadowSections == null) sodium$shadowSections = new ChunkSection[sections.length];
        if (sodium$shadowMasks == null) sodium$shadowMasks = new BitSet[sections.length];
        for (int i = 0; i < sections.length; i++) {
            sodium$shadowSections[i] = sections[i] != null ? new ChunkSection(i * 16) : null;
            sodium$shadowMasks[i] = null;
        }
        boolean f;
        var bp = new BlockPos.Mutable();
        for (int sy = 0; sy < 16; sy++) {
            var section = sections[sy];
            if (section == null) continue;
            var shadowSection = sodium$shadowSections[sy];
            var shadowMask = sodium$shadowMasks[sy];
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    var smear = biomeArray.getBiomeForNoiseGen(x >> 2, sy << 2, z >> 2).getSurfaceConfig().getUnderMaterial();
                    for (int y = 0; y < 16; y++) {
                        var state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        if (state.isIn(SodiumHelper.HIDDEN_BLOCKS)) {
                            f = state.getBlock() instanceof InfestedBlock;
                            if (f || sodium$isBlockHidden(state, bp.set(x, (sy << 4) | y, z))) {
                                shadowSection.setBlockState(x, y, z, f ? ((InfestedBlock) state.getBlock()).getRegularBlock().getDefaultState() : smear);
                                getOrCreateMask(sodium$shadowMasks, sy).set(toIndex(x, y, z), true);
                            } else {
                                shadowSection.setBlockState(x, y, z, state);
                            }
                        } else {
                            shadowSection.setBlockState(x, y, z, state);
                            if (state.getFluidState().isEmpty() && !state.getBlock().hasBlockEntity() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) {
                                smear = state;
                            }
                        }
                    }
                }
        }
    }

    private BlockState sodium$getSmearBlock(BlockPos pos, BlockPos.Mutable bp) {
        BlockState state;
        if ((!(state = sodium$getShadowBlock(bp.set(pos, Direction.DOWN))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) ||
                (!(state = sodium$getShadowBlock(bp.set(pos, Direction.UP))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) ||
                (!(state = sodium$getShadowBlock(bp.set(pos, Direction.NORTH))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) ||
                (!(state = sodium$getShadowBlock(bp.set(pos, Direction.SOUTH))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) ||
                (!(state = sodium$getShadowBlock(bp.set(pos, Direction.EAST))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)) ||
                (!(state = sodium$getShadowBlock(bp.set(pos, Direction.WEST))).isAir() && !state.isIn(SodiumHelper.NO_SMEAR_BLOCKS)))
            return state;
        Main.log.error("Block is hidden yet smear failed to get a surrounding block?! {} -> {} @ {}", world, getBlockState(pos), pos);
        if (world.method_27983().equals(DimensionType.OVERWORLD_REGISTRY_KEY)) Blocks.STONE.getDefaultState();
        return biomeArray.getBiomeForNoiseGen(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).getSurfaceConfig().getUnderMaterial();
    }

    public BlockState sodium$getShadowBlock(BlockPos pos) {
        int i = pos.getX(), j = pos.getY(), k = pos.getZ();
        try {
            if (j >= 0 && j >> 4 < sodium$shadowSections.length) {
                var section = sodium$shadowSections[j >> 4];
                return ChunkSection.isEmpty(section) ? Blocks.AIR.getDefaultState() : section.getBlockState(i & 15, j & 15, k & 15);
            }
            return Blocks.VOID_AIR.getDefaultState();
        } catch (Throwable t) {
            var report = CrashReport.create(t, "Sodium: Getting shadow block state");
            report.addElement("Block being got")
                    .add("Location", () -> CrashReportSection.createPositionString(i, j, k))
                    .add("Section", () -> "Section " + (j >> 4) + ": " + (sodium$shadowSections == null ? "shadowSections -> null?" : sodium$shadowSections[j >> 4]));
            throw new CrashException(report);
        }
    }

    public ChunkSection[] sodium$getShadowSections() {
        if (!ArrayUtils.isEmpty(sections) && ArrayUtils.isEmpty(sodium$shadowSections))
            sodium$generateShadow();
        return sodium$shadowSections;
    }

    public boolean sodium$isMasked(BlockPos pos) {
        var mask = sodium$shadowMasks[pos.getY() >> 4];
        return mask != null && mask.get((pos.getY() & 15) << 8 | (pos.getX() & 15) << 4 | pos.getZ() & 15);
    }
}
