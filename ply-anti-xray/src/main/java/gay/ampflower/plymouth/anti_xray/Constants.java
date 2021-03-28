package gay.ampflower.plymouth.anti_xray;

import gay.ampflower.plymouth.anti_xray.mixins.AccessorBlockTag;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
public final class Constants {
    public static final Logger LOGGER = LogManager.getLogger("Plymouth Anti-Xray");

    public static final Tag<Block>
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
}
