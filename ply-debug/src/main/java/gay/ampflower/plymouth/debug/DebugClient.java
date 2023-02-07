package gay.ampflower.plymouth.debug;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import gay.ampflower.plymouth.debug.anti_xray.AntiXrayClientDebugger;
import gay.ampflower.plymouth.debug.misc.BoundingBoxDebugClient;
import gay.ampflower.plymouth.debug.misc.MiscDebugClient;

import static gay.ampflower.plymouth.debug.Debug.tryOrLog;

/**
 * The primary initializer for the debug client.
 *
 * @author Ampflower
 * @since 0.0.0
 */
@Environment(EnvType.CLIENT)
public class DebugClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        tryOrLog(AntiXrayClientDebugger::initialise, "AntiXray client debugger cannot be loaded.");
        tryOrLog(MiscDebugClient::initialise, "Misc client debugger cannot be loaded.");
        tryOrLog(BoundingBoxDebugClient::initialise, "Bounding box client debugger cannot be loaded.");
    }
}
