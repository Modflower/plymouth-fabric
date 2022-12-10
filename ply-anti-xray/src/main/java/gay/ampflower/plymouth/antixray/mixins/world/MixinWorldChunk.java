package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.LazyChunkManager;
import gay.ampflower.plymouth.antixray.ShadowChunk;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.InfestedBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
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

import java.util.*;

import static gay.ampflower.plymouth.antixray.Constants.HIDDEN_BLOCKS;
import static gay.ampflower.plymouth.antixray.Constants.LOGGER;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk extends Chunk implements ShadowChunk {
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
    // A bit set is used for saving memory in a compact manner, taking only up to 8 KiB per chunk in a standard world.
    @Unique
    private BitSet shadowMask;

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
            int yi = getSectionIndex(y);
            logic:
            if (shadowSections[yi] != null || state.isIn(HIDDEN_BLOCKS)) {
                var index = toIndex(i, y, k);
                if (state.isIn(HIDDEN_BLOCKS)) {
                    if (isMasked(index)) return;
                    var mutPos = pos.mutableCopy();
                    if (isBlockHidden(state, mutPos)) {
                        setMasked(index);
                        if (isHidingCandidate(old, pos)) return;
                        state = getSmearBlock(mutPos.set(pos), state, null);
                    }
                } else {
                    setUnmasked(index);
                    if (shadowSections[yi] == null) {
                        break logic;
                    }
                }
                getShadowSection(yi, true).setBlockState(i, j, k, state);
            }
            trackUpdate(pos);
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
        var c = ((LazyChunkManager) world.getChunkManager()).plymouth$getChunkLazy(cx, cz);
        return c == null ? Blocks.STONE.getDefaultState() : c.getBlockState(bp);
    }

    @Unique
    private void setIfHidden(final BlockPos.Mutable bp) {
        // This is called by other chunks, shadowMask check is required.
        if (isOutOfHeightLimit(bp) || shadowMask == null) return;
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                cx = x >> 4, cy = getSectionIndex(y), cz = z >> 4;
        if (pos.x == cx && pos.z == cz) {
            var section = sectionArray[cy];
            if (isSectionEmpty(section) || !section.hasAny(s -> s.isIn(HIDDEN_BLOCKS))) return;
            int ox = x & 15, oy = y & 15, oz = z & 15;
            var state = section.getBlockState(ox, oy, oz);
            if (state.isIn(HIDDEN_BLOCKS)) {
                if (isMasked(toIndex(x, y, z))) {
                    if (!isBlockHidden(state, bp.set(ox, y, oz)))
                        plymouth$unmaskBlock(bp.set(x, y, z));
                } else if (isBlockHidden(state, bp.set(ox, y, oz))) {
                    var smear = getSmearBlock(bp.set(ox, y, oz), state, null);
                    plymouth$maskBlock(bp.set(x, y, z), smear);
                }
            }
        } else {
            if (world.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof MixinWorldChunk worldChunk)
                worldChunk.setIfHidden(bp);
        }
    }

    @Unique
    private boolean isBlockHidden(final BlockState state,
                                  final BlockPos.Mutable bp) {
        if (state.isAir()) return false;
        // TODO: Account for light levels if Overworld.
        int x = bp.getX(), y = bp.getY(), z = bp.getZ(),
                i = (pos.x << 4) | x, k = (pos.z << 4) | z;
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
            proxiedSections = Arrays.copyOf(sectionArray, sectionArray.length);
        } else for (int i = 0; i < sectionArray.length; i++) {
            shadowSections[i] = null;
        }
        if (shadowMask == null) {
            // Default to 4K as most chunks will immediately saturate this area.
            shadowMask = new BitSet(4096);
        } else {
            shadowMask.clear();
        }

        var bp = new BlockPos.Mutable();
        for (int sy = 0, syl = sectionArray.length; sy < syl; sy++) {
            var section = sectionArray[sy];
            if (isSectionEmpty(section)) continue;
            if (section.hasAny(s -> s.isIn(HIDDEN_BLOCKS))) {
                var shadowSection = getShadowSection(sy, false);
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++) {
                        var smear = Blocks.VOID_AIR.getDefaultState();
                        for (int y = 0; y < 16; y++) {
                            var state = section.getBlockState(x, y, z);
                            if (state.isAir()) continue;
                            int oy = (sy << 4) + getBottomY() + y;
                            if (state.isIn(HIDDEN_BLOCKS)) {
                                if (isBlockHidden(state, bp.set(x, oy, z))) {
                                    state = getSmearBlock(bp.set(x, oy, z), state, smear);
                                    setMasked(toIndex(sy, x, y, z));
                                }
                            } else if (isHidingCandidate(state, bp.set(x, oy, z))) {
                                smear = state;
                            }
                            shadowSection.setBlockState(x, y, z, state);
                        }
                    }
            }
        }
    }

    private BlockState getSmearBlock(BlockPos.Mutable bp, BlockState state, BlockState fallback) {
        if (state.getBlock() instanceof InfestedBlock infestedBlock) {
            state = infestedBlock.toRegularState(state);
            if (!state.isIn(HIDDEN_BLOCKS)) {
                return state;
            }
        }
        if (fallback != null && !fallback.isAir()) {
            return fallback;
        }
        int x = bp.getX(), y = bp.getY(), z = bp.getZ();
        if (isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y - 1, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y + 1, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z - 1)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x, y, z + 1)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x + 1, y, z)), bp) ||
                isHidingCandidate(state = plymouth$getShadowBlock(bp.set(x - 1, y, z)), bp))
            return state;
        bp.set(x, y, z);
        RegistryKey<World> registryKey = world.getRegistryKey();
        if (World.NETHER.equals(registryKey)) {
            return Blocks.NETHERRACK.getDefaultState();
        } else if (World.END.equals(registryKey)) {
            return Blocks.END_STONE.getDefaultState();
        } else if (bp.getY() >= 0) {
            return Blocks.STONE.getDefaultState();
        } else {
            return Blocks.DEEPSLATE.getDefaultState();
        }
    }

    @NotNull
    public BlockState plymouth$getShadowBlock(BlockPos pos) {
        if (proxiedSections == null) return Blocks.VOID_AIR.getDefaultState();
        int i = pos.getX(), j = pos.getY(), k = pos.getZ(), l = getSectionIndex(j);
        try {
            if (l >= 0 && l < proxiedSections.length) {
                var section = proxiedSections[l];
                return isSectionEmpty(section) ? Blocks.AIR.getDefaultState() : section.getBlockState(i & 15, j & 15, k & 15);
            }
            return Blocks.VOID_AIR.getDefaultState();
        } catch (Throwable t) {
            var report = CrashReport.create(t, "Plymouth: Anti-Xray: Getting shadow block state");
            report.addElement("Block being got")
                    .add("Location", () -> CrashReportSection.createPositionString(world, i, j, k))
                    .add("Section", () -> "Section " + l + ": " + (proxiedSections == null ? "proxiedSections -> null?" : proxiedSections[l]));
            throw new CrashException(report);
        }
    }

    @Override
    public @Nullable BlockEntity plymouth$getShadowBlockEntity(BlockPos pos) {
        if (shadowSections == null || plymouth$isMasked(pos)) return null;
        return blockEntities.get(pos);
    }

    @Override
    public void plymouth$unmaskBlock(BlockPos pos) {
        if (plymouth$isMasked(pos)) {
            final int x = pos.getX(), y = pos.getY(), z = pos.getZ(), cy = getSectionIndex(y);
            getShadowSection(cy, true).setBlockState(x & 15, y & 15, z & 15, sectionArray[cy].getBlockState(x & 15, y & 15, z & 15));
            setUnmasked(toIndex(pos));
            trackUpdate(pos);
        }
    }

    @Override
    public void plymouth$maskBlock(BlockPos pos, BlockState state) {
        final int y = pos.getY(), cy = getSectionIndex(y);
        // Do note, we're calling plymouth$getShadowSections() to force generation of the shadow chunk.
        plymouth$getShadowSections();
        getShadowSection(cy, true).setBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15, state);
        setMasked(toIndex(pos));
        trackUpdate(pos);
    }

    @Override
    @NotNull
    public ChunkSection[] plymouth$getShadowSections() {
        if (!ArrayUtils.isEmpty(sectionArray) && ArrayUtils.isEmpty(shadowSections))
            generateShadow();
        return Objects.requireNonNullElse(proxiedSections, sectionArray);
    }

    @Override
    public BitSet plymouth$getShadowMask() {
        return shadowMask;
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
        if (shadowMask == null || isOutOfHeightLimit(pos)) return false;
        return shadowMask.get(toIndex(pos));
    }

    @Override
    public boolean plymouth$isMaskedOrOutOfWorld(BlockPos pos) {
        return shadowMask == null || isOutOfHeightLimit(pos) || shadowMask.get(toIndex(pos));
    }

    @Override
    public boolean plymouth$unsafe$uncheckedUpdate(BlockPos pos) {
        if (plymouth$isMaskedOrOutOfWorld(pos)) return false;
        int j = pos.getY();
        int i = getSectionIndex(j);

        var shadow = shadowSections[i];

        if (shadow != null) {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;

            int y = j & 15;

            var state = sectionArray[i].getBlockState(x, y, z);

            var old = shadow.setBlockState(x, y, z, state);

            if (SharedConstants.isDevelopment) {
                if (state == old) {
                    LOGGER.warn("Unchecked caller unnecessarily updated block {} at {}. Note: This may have been caused by missing write.", state, pos, new Throwable());
                } else {
                    var mutPos = pos.mutableCopy();
                    if (isBlockHidden(state, mutPos)) {
                        LOGGER.warn("Unchecked caller failed hidden check for {} at {}", state, pos, new Throwable());
                    }

                    if (VoxelShapes.matchesAnywhere(state.getCollisionShape(world, pos), old.getCollisionShape(world, pos), BooleanBiFunction.NOT_SAME)) {
                        LOGGER.warn("Unchecked caller failed collisions check for {} against {} at {}", state, old, pos, new Throwable());
                    }
                }
            }
        }

        return true;
    }

    @Unique
    private boolean isMasked(int i) {
        return shadowMask != null && shadowMask.get(i);
    }

    @Unique
    private void setMasked(int i) {
        if (shadowMask == null) return;
        shadowMask.set(i);
    }

    @Unique
    private void setUnmasked(int i) {
        if (shadowMask == null) return;
        shadowMask.clear(i);
    }

    @Unique
    private int toIndex(BlockPos pos) {
        return toIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    @Unique
    private int toIndex(int x, int y, int z) {
        // getBottomY() will probably be omitted once safe to screw with internal APIs.
        return (y - getBottomY()) << 8 | (z & 15) << 4 | (x & 15);
    }

    @Unique
    private int toIndex(int si, int x, int y, int z) {
        return si << 12 | y << 8 | (z & 15) << 4 | (x & 15);
    }

    @Unique
    private void trackUpdate(BlockPos pos) {
        ((ServerWorld) world).getChunkManager().markForUpdate(pos);
    }

    @Unique
    private ChunkSection getShadowSection(int y, boolean copy) {
        return shadowSections[y] == null ? proxiedSections[y] = shadowSections[y] = new ChunkSection(y + getBottomSectionCoord(),
                copy ? clone(sectionArray[y].getBlockStateContainer()) : new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE),
                sectionArray[y].getBiomeContainer()) : shadowSections[y];
    }

    @Unique
    private static <T> PalettedContainer<T> clone(PalettedContainer<T> i) {
        @SuppressWarnings("unchecked")
        var cchi = (AccessorPalettedContainer<T>) i;
        var di = cchi.getData();
        var pi = di.palette();
        if (pi instanceof IdListPalette<T>) {
            return i.copy();
        } else {
            var po = new ArrayList<T>(pi.getSize());
            for (int j = 0, k = pi.getSize(); j < k; j++) po.add(pi.get(j));
            return new PalettedContainer<>(cchi.getIdList(), cchi.getPaletteProvider(), di.configuration(), di.storage().copy(), po);
        }
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
