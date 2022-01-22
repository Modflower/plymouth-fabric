package net.kjp12.plymouth.antixray;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Map;

/**
 * Shadow Chunk interface for chunks. Used for fetching and setting shadow
 * blocks to be sent to the client.
 *
 * @author kjp12
 * @since 0.0.0
 */
public interface ShadowChunk extends ShadowBlockView {
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
    void plymouth$setShadowBlock(BlockPos pos, BlockState state);

    /**
     * Gets the shadow chunk, generating them if they're not present.
     *
     * @return The shadow chunk, always nonnull
     * @see net.kjp12.plymouth.antixray.transformers.GudAsmTransformer
     * @see net.kjp12.plymouth.antixray.transformers.PacketTransformer
     */
    // The return are for the two mass ASM transformers, listed in the See block(s).
    @SuppressWarnings("UnusedReturnValue")
    @NotNull
    ChunkSection[] plymouth$getShadowSections();

    /**
     * Gets the shadow masks. This does not generate them if they're not present.
     *
     * @return The shadow mask. Maybe null.
     */
    BitSet[] plymouth$getShadowMasks();

    /**
     * Gets all visible entities in a chunk.
     *
     * @return The visible entities, always nonnull
     */
    @NotNull
    Map<BlockPos, BlockEntity> plymouth$getShadowBlockEntities();

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
