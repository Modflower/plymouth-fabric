package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.ShadowBlockView;
import gay.ampflower.plymouth.antixray.ShadowChunk;
import gay.ampflower.plymouth.antixray.transformers.GudAsmTransformer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(World.class)
public abstract class MixinWorld implements WorldAccess, ShadowBlockView {
    /**
     * Redirector stub for {@link GudAsmTransformer}.
     *
     * @param pos The position to lookup in the shadow chunk.
     * @return The shadow block.
     */
    @Override
    public @NotNull BlockState plymouth$getShadowBlock(BlockPos pos) {
        if (isOutOfHeightLimit(pos)) {
            return Blocks.VOID_AIR.getDefaultState();
        }
        return ((ShadowChunk) getChunk(pos)).plymouth$getShadowBlock(pos);
    }

    /**
     * Redirector stub for {@link GudAsmTransformer}.
     *
     * @param pos The position to lookup in the shadow chunk.
     * @return The block entity if both existing and visible, else null.
     */
    @Override
    public @Nullable BlockEntity plymouth$getShadowBlockEntity(BlockPos pos) {
        if (isOutOfHeightLimit(pos)) {
            return null;
        }
        return ((ShadowChunk) getChunk(pos)).plymouth$getShadowBlockEntity(pos);
    }
}
