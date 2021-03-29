package net.kjp12.plymouth.anti_xray;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

/**
 * Shadow Chunk interface for chunks. Used for the Anti-Xray feature.
 *
 * @author kjp12
 * @since 0.0.0
 */
public interface IShadowChunk {
    /**
     * Gets the block from the shadow chunk, if the shadow is present.
     *
     * @param pos The position of the block.
     * @return The block at that position, {@link net.minecraft.block.Blocks#VOID_AIR} otherwise.
     */
    @NotNull
    BlockState plymouth$getShadowBlock(BlockPos pos);

    /**
     * Changes the block in the shadow block back to the real block, forcing a server/client sync.
     *
     * @param pos The position of the block to unset.
     */
    void plymouth$unsetShadowBlock(BlockPos pos);

    /**
     * Sets the block in the shadow chunk, forcing a server/client desync.
     * Please use sparingly, as you can create some very weird and otherwise impossible states with this.
     *
     * @param pos   The position of the block to set.
     * @param state The state to set the block to.
     */
    default void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
    }

    /**
     * Gets the shadow chunk, generating them if they're not present.
     *
     * @return The shadow chunk, always nonnull
     */
    @NotNull
    ChunkSection[] plymouth$getShadowSections();

    /**
     * Gets the shadow masks. This does not generate them if they're not present.
     *
     * @return The shadow mask. Maybe null.
     */
    BitSet[] plymouth$getShadowMasks();

    /**
     * Checks if the block is shadowed.
     *
     * @return true if shadow is present and the block at that position is shadowed, false otherwise.
     */
    boolean plymouth$isMasked(BlockPos pos);

    /**
     * Checks if the block would cull the adjacent block from the given direction.
     *
     * @param state The block to check against.
     * @param from  The direction the block is adjacent with.
     * @param pos   The position of the block. Used for shape and lava.
     * @return true if the block would cull the adjacent block from the given direction, false otherwise.
     */
    boolean plymouth$isCulling(BlockState state, Direction from, BlockPos pos);

    // Dangling method for debug mixin to hook into.

    /**
     * Forces a block update.
     *
     * @param pos The position to force an update.
     * @implNote Classes implementing this <em>should</em> call super to allow for debugging hooks.
     */
    default void plymouth$trackUpdate(BlockPos pos) {
    }

    default boolean plymouth$isBlockHidden(BlockState state, BlockPos.Mutable pos) {
        return false;
    }
}
