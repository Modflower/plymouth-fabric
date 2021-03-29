package gay.ampflower.plymouth.debug.mixins;

import gay.ampflower.plymouth.anti_xray.IShadowChunk;
import gay.ampflower.plymouth.debug.Debug;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * @author Ampflower
 * @since 0.0.0
 */
@Pseudo
@Mixin(value = IShadowChunk.class, remap = false)
public interface MetaMixinShadowChunk {
    /**
     * @author Ampflower
     */
    @Overwrite
    default void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        Debug.send(Debug.debugAntiXraySet, pos.asLong());
    }

    /**
     * @author Ampflower
     */
    @Overwrite
    default void plymouth$trackUpdate(BlockPos pos) {
        Debug.send(Debug.debugAntiXrayUpdate, pos.asLong());
    }

    /**
     * @author Ampflower
     */
    @Overwrite
    default boolean plymouth$isBlockHidden(BlockState state, BlockPos.Mutable pos) {
        Debug.send(Debug.debugAntiXrayTest, pos.asLong());
        return false;
    }
}
