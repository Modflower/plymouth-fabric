package gay.ampflower.plymouth.tracker;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public final class Tracker {
    public static final Logger logger = LogManager.getLogger("Plymouth: Tracker");

    public static final Text
            INSPECT_START = new TranslatableText("commands.plymouth.tracker.inspect.start"),
            INSPECT_END = new TranslatableText("commands.plymouth.tracker.inspect.end");

    public static void init() {
        CommandRegistrationCallback.EVENT.register(TrackerCommand::register);
    }
}
