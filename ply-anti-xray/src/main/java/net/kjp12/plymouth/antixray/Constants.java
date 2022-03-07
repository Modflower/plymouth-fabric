package net.kjp12.plymouth.antixray;// Created Mar. 02, 2021 @ 22:16

import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
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
    public static final TagKey<Block> HIDDEN_BLOCKS = TagKey.of(Registry.BLOCK_KEY, Identifier.tryParse("plymouth-anti-xray:hidden"));

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
