package net.kjp12.plymouth.antixray.mixins.world;

import net.kjp12.plymouth.antixray.Constants;
import net.kjp12.plymouth.antixray.IShadowChunk;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.chunk.BlendingData;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.kjp12.plymouth.antixray.Constants.HIDDEN_BLOCKS;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk extends Chunk implements IShadowChunk {
    @Shadow
    @Final
    World world;
    /**
     * Sections that represent the anti-xray view of the world.
     */
    @Unique
    private ChunkSection[] shadowSections;
    /**
     * Sections sent to the client. This will be made up of shadow if non-null, else original.
     */
    @Unique
    private ChunkSection[] proxiedSections;
    // Used for block-entity fetching for the client.
    // May also be used to invalidate entities?
    // 16 separate sets instead of one massive set meant for saving memory when the corresponding section isn't present.
    @Unique
    private BitSet[] shadowMasks;

    public MixinWorldChunk(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void plymouth$setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> bs, int y, ChunkSection chunkSection, boolean emptySection, int i, int j, int k, BlockState old) {
        if (world instanceof ServerWorld && shadowSections != null) {
            var index = Constants.toIndex(i, j, k);
            int yi = getSectionIndex(y);
            var mask = getShadowMask(yi);
            logic:
            if (state.getBlock() instanceof InfestedBlock infestedBlock) {
                if (mask.get(index)) return;
                // This will always be true regardless of what happens in this branch.
                mask.set(index, true);
                var regular = infestedBlock.toRegularState(state);
                if (regular.isIn(HIDDEN_BLOCKS)) {
                    var mutPos = pos.mutableCopy();
                    if (plymouth$isBlockHidden(regular, mutPos)) {
                        state = getSmearBlock(mutPos.set(pos));
                        break logic;
                    }
                }
                state = regular;
            } else if (state.isIn(HIDDEN_BLOCKS)) {
                if (mask.get(index)) return;
                var mutPos = pos.mutableCopy();
                if (plymouth$isBlockHidden(state, mutPos)) {
                    if (isHidingCandidate(old, pos)) return;
                    state = getSmearBlock(mutPos.set(pos));
                    mask.set(index, true);
                }
            } else {
                mask.set(index, false);
            }
            getShadowSection(yi).setBlockState(i, j, k, state);
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
        if (isOutOfHeightLimit(bp) || shadowMasks == null) return;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                cx = x >> 4, cy = getSectionIndex(y), cz = z >> 4;
        if (pos.x == cx && pos.z == cz) {
            var section = sectionArray[cy];
            if (isSectionEmpty(section) || !section.hasAny(s -> s.isIn(HIDDEN_BLOCKS))) return;
            int ox = x & 15, oy = y & 15, oz = z & 15;
            var state = section.getBlockState(ox, oy, oz);
            if (state.getBlock() instanceof InfestedBlock infestedBlock) {
                plymouth$setShadowBlock(bp, infestedBlock.toRegularState(state));
            } else if (state.isIn(HIDDEN_BLOCKS)) {
                if (getShadowMask(cy).get(Constants.toIndex(ox, oy, oz))) {
                    if (!plymouth$isBlockHidden(state, bp.set(ox, y, oz)))
                        plymouth$unsetShadowBlock(bp.set(x, y, z));
                } else if (plymouth$isBlockHidden(state, bp.set(ox, y, oz))) {
                    var smear = getSmearBlock(bp.set(ox, y, oz));
                    plymouth$setShadowBlock(bp.set(x, y, z), smear);
                }
            }
        } else {
            if (world.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof MixinWorldChunk worldChunk)
                worldChunk.setIfHidden(bp);
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
        if (shadowSections == null) {
            shadowSections = new ChunkSection[sectionArray.length];
            proxiedSections = sectionArray.clone();
        } else for (int i = 0; i < sectionArray.length; i++) {
            shadowSections[i] = null;
        }
        if (shadowMasks == null) {
            shadowMasks = new BitSet[sectionArray.length];
        } else for (int i = 0; i < sectionArray.length; i++) {
            shadowMasks[i] = null;
        }

        var bp = new BlockPos.Mutable();
        for (int sy = 0, syl = sectionArray.length; sy < syl; sy++) {
            var section = sectionArray[sy];
            if (isSectionEmpty(section)) continue;
            if (section.hasAny(s -> s.isIn(HIDDEN_BLOCKS))) {
                var shadowSection = getShadowSection(sy);
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++) {
                        var smear = Blocks.VOID_AIR.getDefaultState();
                        for (int y = 0; y < 16; y++) {
                            var state = section.getBlockState(x, y, z);
                            if (state.isAir()) continue;
                            if (state.getBlock() instanceof InfestedBlock infestedBlock) {
                                state = infestedBlock.toRegularState(state);
                                getShadowMask(sy).set(Constants.toIndex(x, y, z), true);
                            } else if (state.isIn(HIDDEN_BLOCKS)) {
                                if (plymouth$isBlockHidden(state, bp.set(x, (sy << 4) + getBottomY() + y, z))) {
                                    state = smear.isAir() ? getSmearBlock(bp.set(x, (sy << 4) + getBottomY(), z)) : smear;
                                    getShadowMask(sy).set(Constants.toIndex(x, y, z), true);
                                }
                            } else if (isHidingCandidate(state, bp.set(x, (sy << 4) + getBottomY() + y, z))) {
                                smear = state;
                            }
                            shadowSection.setBlockState(x, y, z, state);
                        }
                    }
            }
            // // No longer necessary due to proxy setup.
            // else {
            //     // We'll just clone the palette as is.
            //     proxiedSections[sy] = shadowSections[sy] = (ChunkSection) ((CloneAccessible) section).clone();
            // }
        }
    }

    @Unique
    private BlockState getSmearBlock(BlockPos.Mutable bp) {
        int x = bp.getX(), y = bp.getY(), z = bp.getZ();
        BlockState state;
        if (isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y - 1, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y + 1, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z - 1)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z + 1)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x + 1, y, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x - 1, y, z)), bp))
            return state;
        bp.set(x, y, z);
        return getDefaultSmearBlock(bp);
    }

    @Unique
    private BlockState getDefaultSmearBlock(BlockPos pos) {
        RegistryKey<World> registryKey = world.getRegistryKey();
        if (World.NETHER.equals(registryKey)) {
            return Blocks.NETHERRACK.getDefaultState();
        } else if (World.END.equals(registryKey)) {
            return Blocks.END_STONE.getDefaultState();
        } else if (pos.getY() >= 0) {
            return Blocks.STONE.getDefaultState();
        } else {
            return Blocks.DEEPSLATE.getDefaultState();
        }
    }

    @NotNull
    public BlockState plymouth$getShadowBlock(BlockPos pos) {
        if (shadowSections == null) return Blocks.VOID_AIR.getDefaultState();
        int i = pos.getX(), j = pos.getY(), k = pos.getZ(), l = getSectionIndex(j);
        try {
            if (l >= 0 && l < shadowSections.length) {
                var section = shadowSections[l];
                return isSectionEmpty(section) ? Blocks.AIR.getDefaultState() : section.getBlockState(i & 15, j & 15, k & 15);
            }
            return Blocks.VOID_AIR.getDefaultState();
        } catch (Throwable t) {
            var report = CrashReport.create(t, "Plymouth: Anti-Xray: Getting shadow block state");
            report.addElement("Block being got")
                    .add("Location", () -> CrashReportSection.createPositionString(world, i, j, k))
                    .add("Section", () -> "Section " + l + ": " + (shadowSections == null ? "shadowSections -> null?" : shadowSections[l]));
            throw new CrashException(report);
        }
    }

    @Override
    public @Nullable BlockEntity plymouth$getShadowBlockEntity(BlockPos pos) {
        if (shadowSections == null || plymouth$isMasked(pos)) return null;
        return blockEntities.get(pos);
    }

    @Override
    public void plymouth$unsetShadowBlock(BlockPos pos) {
        if (shadowMasks == null) return; // There is nothing to unset.
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ(), cy = getSectionIndex(y), i = Constants.toIndex(pos);
        final var m = getShadowMask(cy);
        if (m.get(i)) {
            getShadowSection(cy).setBlockState(x & 15, y & 15, z & 15, sectionArray[cy].getBlockState(x & 15, y & 15, z & 15));
            m.set(i, false);
            plymouth$trackUpdate(pos);
        }
    }

    @Override
    public void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        IShadowChunk.super.plymouth$setShadowBlock(pos, state);
        final int y = pos.getY(), cy = getSectionIndex(y);
        // Do note, we're calling plymouth$getShadowSections() to force generation of the shadow chunk.
        plymouth$getShadowSections();
        getShadowSection(cy).setBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15, state);
        getShadowMask(cy).set(Constants.toIndex(pos), true);
        plymouth$trackUpdate(pos);
    }

    @Override
    @NotNull
    public ChunkSection[] plymouth$getShadowSections() {
        if (!ArrayUtils.isEmpty(sectionArray) && ArrayUtils.isEmpty(shadowSections))
            generateShadow();
        return Objects.requireNonNullElse(proxiedSections, sectionArray);
    }

    @Override
    public BitSet[] plymouth$getShadowMasks() {
        return shadowMasks;
    }

    @Override
    public @NotNull Map<BlockPos, BlockEntity> plymouth$getShadowBlockEntities() {
        var map = new HashMap<BlockPos, BlockEntity>();
        blockEntities.forEach((pos, tile) -> {
            if (!plymouth$isMasked(pos)) map.put(pos, tile);
        });
        return map;
    }

    @Override
    public boolean plymouth$isMasked(BlockPos pos) {
        if (shadowMasks == null) return false;
        var mask = shadowMasks[getSectionIndex(pos.getY())];
        return mask != null && mask.get(Constants.toIndex(pos));
    }

    @Override
    public void plymouth$trackUpdate(BlockPos pos) {
        IShadowChunk.super.plymouth$trackUpdate(pos);
        ((ServerWorld) world).getChunkManager().markForUpdate(pos);
    }

    @Unique
    private ChunkSection getShadowSection(int y) {
        return shadowSections[y] == null ? proxiedSections[y] = shadowSections[y] = new ChunkSection((y << 4) + getBottomY(), world.getRegistryManager().get(Registry.BIOME_KEY)) : shadowSections[y];
    }

    @Unique
    private BitSet getShadowMask(int y) {
        return shadowMasks[y] == null ? shadowMasks[y] = new BitSet(4096) : shadowMasks[y];
    }

    @Unique
    private boolean isHidingCandidate(BlockState state, BlockPos pos) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && !state.isIn(HIDDEN_BLOCKS) &&
                !VoxelShapes.matchesAnywhere(state.getCollisionShape(this, pos), VoxelShapes.fullCube(), BooleanBiFunction.NOT_SAME);
    }

    @Unique
    private static boolean isSectionEmpty(ChunkSection section) {
        return section == null || section.isEmpty();
    }
}
