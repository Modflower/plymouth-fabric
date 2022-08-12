package gay.ampflower.plymouth.tracker;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public final class Tracker {
    public static final Logger logger = LoggerFactory.getLogger("Plymouth: Tracker");

    public static final Text
            INSPECT_START = Text.translatable("commands.plymouth.tracker.inspect.start"),
            INSPECT_END = Text.translatable("commands.plymouth.tracker.inspect.end");

    public static void init() {
        CommandRegistrationCallback.EVENT.register(TrackerCommand::register);
    }
}
