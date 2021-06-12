package gay.ampflower.plymouth.tracker.glue;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.Optional;

/**
 * @author Ampflower
 * @vanilla-copy {@link net.minecraft.block.RespawnAnchorBlock#explode(BlockState, World, BlockPos)}
 * @since ${version}
 **/
public class RespawnAnchorExplosionBehavior extends ExplosionBehavior {
    private BlockPos explodedPos;
    private boolean bl2;

    public RespawnAnchorExplosionBehavior(BlockPos explodedPos, boolean bl2) {
        this.explodedPos = explodedPos;
        this.bl2 = bl2;
    }

    public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
        return pos.equals(explodedPos) && bl2 ? Optional.of(Blocks.WATER.getBlastResistance()) : super.getBlastResistance(explosion, world, pos, blockState, fluidState);
    }
}
