package gay.ampflower.sodium.helpers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

import java.util.List;

public interface IShadowChunk {
    BlockState sodium$getShadowBlock(BlockPos pos);

    ChunkSection[] sodium$getShadowSections();

    List<BlockEntity> sodium$getVisibleTileEntities();

    boolean sodium$isMasked(BlockPos pos);
}
