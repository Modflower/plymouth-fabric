package gay.ampflower.helium.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

public interface IShadowChunk {
    BlockState helium$getShadowBlock(BlockPos pos);

    ChunkSection[] helium$getShadowSections();

    boolean helium$isMasked(BlockPos pos);
}
