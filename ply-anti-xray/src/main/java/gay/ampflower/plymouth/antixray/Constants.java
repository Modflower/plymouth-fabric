package gay.ampflower.plymouth.antixray;

import gay.ampflower.plymouth.antixray.mixins.AccessorBlockTag;
import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
public final class Constants {
    public static final Logger LOGGER = LogManager.getLogger("Plymouth Anti-Xray");

    /**
     * @deprecated Will be replaced by a different configuration scheme later on.
     */
    @Deprecated
    public static final TagKey<Block> HIDDEN_BLOCKS;

    static {
        HIDDEN_BLOCKS = AccessorBlockTag.invokeRegister("plymouth-anti-xray:hidden");
    }

    private Constants() {
    }

    public static void init() {
    }

    public static int toIndex(BlockPos pos) {
        return toIndex(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int toIndex(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }
}
