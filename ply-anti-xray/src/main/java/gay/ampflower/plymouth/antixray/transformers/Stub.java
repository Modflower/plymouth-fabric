package gay.ampflower.plymouth.antixray.transformers;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Provider stub for {@link GudAsmTransformer} to read.
 * <p>
 * This class is to never be loaded for use.
 *
 * @author Ampflower
 * @since ${version}
 **/
class Stub {
    BlockState world(World world, BlockPos pos) {
        return world.getBlockState(pos);
    }

    PlayerActionResponseS2CPacket actionResponse(BlockPos pos) {
        return new PlayerActionResponseS2CPacket(pos, null, null, false, null);
    }

    static {
        //noinspection ConstantConditions
        if (true) throw new AssertionError("This class should never load.");
    }
}
