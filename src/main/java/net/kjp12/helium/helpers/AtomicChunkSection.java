package net.kjp12.helium.helpers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtHelper;

import static net.kjp12.helium.Helium.BLOCK_STATE_PALETTE;

/**
 * ChunkSection designed for concurrent read and write access.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class AtomicChunkSection {
    private final int yoff;
    private final short nonEmptyBlockCount;
    private final short randomTickableBlockCount;
    private final short nonEmptyFluidCount;
    private final AtomicPalettedContainer<BlockState> container;

    public AtomicChunkSection(int yoff, short nonEmptyBlockCount, short randomTickableBlockCount, short nonEmptyFluidCount) {
        this.yoff = yoff;
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.randomTickableBlockCount = randomTickableBlockCount;
        this.nonEmptyFluidCount = nonEmptyFluidCount;
        this.container = new AtomicPalettedContainer<>(BLOCK_STATE_PALETTE, Block.STATE_IDS, NbtHelper::toBlockState, NbtHelper::fromBlockState, Blocks.AIR.getDefaultState());
    }
}
