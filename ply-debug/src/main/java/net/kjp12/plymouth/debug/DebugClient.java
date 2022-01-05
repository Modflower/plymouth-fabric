package net.kjp12.plymouth.debug;// Created 2021-03-28T23:15:07

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kjp12.plymouth.debug.anti_xray.AntiXrayClientDebugger;

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
        try {
            AntiXrayClientDebugger.initialise();
        } catch (NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError error) {
            Debug.logger.error("AntiXray debugger cannot be loaded.", error);
        }
    }
}
