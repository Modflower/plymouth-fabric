package net.kjp12.plymouth.tracker;// Created 2021-03-30T05:27:29

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author KJP12
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
