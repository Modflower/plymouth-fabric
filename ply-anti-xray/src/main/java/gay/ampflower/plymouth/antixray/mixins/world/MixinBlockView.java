package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.ShadowBlockView;
import gay.ampflower.plymouth.antixray.transformers.GudAsmTransformer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(BlockView.class)
public interface MixinBlockView extends ShadowBlockView {
    @Shadow
    @Nullable BlockEntity getBlockEntity(BlockPos pos);

    @Shadow
    BlockState getBlockState(BlockPos pos);

    /**
     * Redirector stub for {@link GudAsmTransformer}.
     *
     * @param pos The position to lookup in the shadow chunk.
     * @return The shadow block if applicable, else the standard view.
     */
    // This doesn't need any interface stub as it'll be called directly from asm.
    default @NotNull BlockState plymouth$getShadowBlock(BlockPos pos) {
        return getBlockState(pos);
    }

    /**
     * Redirector stub for {@link GudAsmTransformer}.
     *
     * @param pos The position to lookup in the shadow chunk.
     * @return The block entity if both existing and visible if applicable, else null.
     */
    // This doesn't need any interface stub as it'll be called directly from asm.
    default @Nullable BlockEntity plymouth$getShadowBlockEntity(BlockPos pos) {
        return getBlockEntity(pos);
    }
}
