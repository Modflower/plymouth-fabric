package gay.ampflower.helium;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * A helper file that does not invoke any of net.minecraft.
 * <p>
 * A note to maintainers: This file must <em>not</em> have any references to classes within Minecraft and other mods
 * that are not also an early riser, be it direct or indirect.
 * This class rises early via {@link MixinConfig} and can cause issues to mixins loaded in later.
 * <p>
 * Safe calls include java.*, sun.*, com.sun.*, {@link HeliumEarlyRiser}, {@link MixinConfig} and mods' early riser classes.
 * Any other call is unsafe and may cause failure conditions from classes loading without expected injections.
 *
 * @author Ampflower
 * @since Dec. 19, 2020 @ 18:27
 **/
public class HeliumEarlyRiser {
    public static final Path config = Path.of(".", "config");
    public static final Logger LOGGER = LogManager.getLogger("Helium");
}
