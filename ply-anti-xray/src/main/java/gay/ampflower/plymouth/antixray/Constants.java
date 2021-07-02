package gay.ampflower.plymouth.antixray;

import gay.ampflower.plymouth.antixray.mixins.AccessorBlockTag;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
public final class Constants {
    public static final Logger LOGGER = LogManager.getLogger("Plymouth Anti-Xray");

    public static final Tag.Identified<Block>
            HIDDEN_BLOCKS,
            NO_SMEAR_BLOCKS;

    static {
        //TODO: Replace with Fabric API when it becomes a thing.
        var accessor = AccessorBlockTag.getRequiredTags();
        HIDDEN_BLOCKS = accessor.add("plymouth-anti-xray:hidden");
        NO_SMEAR_BLOCKS = accessor.add("plymouth-anti-xray:no_smear");
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
