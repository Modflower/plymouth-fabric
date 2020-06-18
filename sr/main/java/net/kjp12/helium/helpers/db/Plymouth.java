package net.kjp12.helium.helpers.db;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface Plymouth {
    void replaceBlock(BlockPos pos, BlockState o, BlockState n, Entity breaker);
}
