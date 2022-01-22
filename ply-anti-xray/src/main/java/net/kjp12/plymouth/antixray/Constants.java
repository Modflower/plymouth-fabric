package net.kjp12.plymouth.antixray;// Created Mar. 02, 2021 @ 22:16

import net.kjp12.plymouth.antixray.mixins.AccessorBlockTag;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author KJP12
 * @since 0.0.0
 **/
public final class Constants {
    public static final Logger LOGGER = LogManager.getLogger("Plymouth Anti-Xray");

    /**
     * @deprecated Will be replaced by a different configuration scheme later on.
     */
    @Deprecated
    public static final Tag.Identified<Block>
            HIDDEN_BLOCKS,
            NO_SMEAR_BLOCKS;

    static {
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
