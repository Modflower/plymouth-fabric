package net.kjp12.plymouth.antixray;// Created 2021-20-12T01:11:02

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ShadowBlockView {
    /**
     * Gets the block from the shadow chunk, if the shadow is present.
     *
     * @param pos The position of the block.
     * @return The block at that position, {@link net.minecraft.block.Blocks#VOID_AIR} otherwise.
     */
    @NotNull
    BlockState plymouth$getShadowBlock(BlockPos pos);

    /**
     * Gets the block entity from the shadow chunk, if the shadow is present.
     *
     * @param pos The position of the block.
     * @return The block entity at that position if it exists and not hidden, null otherwise.
     */
    @Nullable BlockEntity plymouth$getShadowBlockEntity(BlockPos pos);
}
