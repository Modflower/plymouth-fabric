package gay.ampflower.plymouth.debug.mixins.anti_xray;

import gay.ampflower.plymouth.anti_xray.IShadowChunk;
import gay.ampflower.plymouth.debug.Debug;
import gay.ampflower.plymouth.debug.anti_xray.AntiXrayDebugger;
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
     * @reason Required to inject into an interface.
     */
    @Overwrite
    default void plymouth$setShadowBlock(BlockPos pos, BlockState state) {
        Debug.send(AntiXrayDebugger.debugAntiXraySet, pos.asLong());
    }

    /**
     * @author Ampflower
     * @reason Required to inject into an interface.
     */
    @Overwrite
    default void plymouth$trackUpdate(BlockPos pos) {
        Debug.send(AntiXrayDebugger.debugAntiXrayUpdate, pos.asLong());
    }

    /**
     * @author Ampflower
     * @reason Required to inject into an interface.
     */
    @Overwrite
    default boolean plymouth$isBlockHidden(BlockState state, BlockPos.Mutable pos) {
        Debug.send(AntiXrayDebugger.debugAntiXrayTest, pos.asLong());
        return false;
    }
}
