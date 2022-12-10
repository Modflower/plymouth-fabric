package net.kjp12.plymouth.debug;// Created 2021-03-28T23:15:07

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kjp12.plymouth.debug.anti_xray.AntiXrayClientDebugger;
import net.kjp12.plymouth.debug.misc.MiscDebugClient;

import static net.kjp12.plymouth.debug.Debug.tryOrLog;

/**
 * The primary initializer for the debug client.
 *
 * @author KJP12
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        tryOrLog(AntiXrayClientDebugger::initialise, "AntiXray client debugger cannot be loaded.");
        tryOrLog(MiscDebugClient::initialise, "Misc client debugger cannot be loaded.");
    }
}
