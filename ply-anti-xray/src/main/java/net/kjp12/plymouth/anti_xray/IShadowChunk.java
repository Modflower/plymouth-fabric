package net.kjp12.plymouth.anti_xray;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;

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
     * Sets the block in the shadow chunk, forcing a server/client desync.
     * Please use sparingly, as you can create some very weird and otherwise impossible states with this.
     *
     * @param pos   The position of the block to set.
     * @param state The state to set the block to.
     */
    void plymouth$setShadowBlock(BlockPos pos, BlockState state);

    /**
     * Gets the shadow chunk, generating them if they're not present.
     *
     * @return The shadow chunk, always nonnull
     */
    @NotNull
    ChunkSection[] plymouth$getShadowSections();

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
}
