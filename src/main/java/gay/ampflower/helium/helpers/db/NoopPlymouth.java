package gay.ampflower.helium.helpers.db;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class NoopPlymouth implements Plymouth {
    @Override
    public void replaceBlock(BlockPos pos, BlockState o, BlockState n, Entity breaker) {

    }
}
