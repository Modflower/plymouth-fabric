package net.kjp12.sodium.helpers;

import net.minecraft.world.chunk.ChunkSection;

public class ShadowChunkSection extends ChunkSection {
    public ShadowChunkSection(int yOffset) {
        super(yOffset);
    }

    public ShadowChunkSection(int yOffset, short nonEmptyBlockCount, short randomTickableBlockCount, short nonEmptyFluidCount) {
        super(yOffset, nonEmptyBlockCount, randomTickableBlockCount, nonEmptyFluidCount);
    }

    public ShadowChunkSection(ChunkSection section) {
        this(section.getYOffset(),
                ((IShadowChunkSection) section).getNonEmptyBlockCount(),
                ((IShadowChunkSection) section).getRandomTickableBlockCount(),
                ((IShadowChunkSection) section).getNonEmptyFluidCount());
    }
}
