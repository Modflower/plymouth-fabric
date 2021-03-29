package net.kjp12.plymouth.debug.mixins;// Created 2021-03-29T02:29:50

import net.kjp12.plymouth.anti_xray.IShadowChunk;
import net.kjp12.plymouth.debug.Debug;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * @author KJP12
 * @since 0.0.0
 */
@Pseudo
@Mixin(value = IShadowChunk.class, remap = false)
public interface MetaMixinShadowChunk {
    /**
     * @author KJP12
     */
    @Overwrite
    default void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        Debug.send(Debug.debugAntiXraySet, pos.asLong());
    }

    /**
     * @author KJP12
     */
    @Overwrite
    default void plymouth$trackUpdate(BlockPos pos) {
        Debug.send(Debug.debugAntiXrayUpdate, pos.asLong());
    }

    /**
     * @author KJP12
     */
    @Overwrite
    default boolean plymouth$isBlockHidden(BlockState state, BlockPos.Mutable pos) {
        Debug.send(Debug.debugAntiXrayTest, pos.asLong());
        return false;
    }
}
