package gay.ampflower.plymouth.debug;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import gay.ampflower.plymouth.debug.anti_xray.AntiXrayClientDebugger;

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
        var loader = FabricLoader.getInstance();
        if (loader.isModLoaded("plymouth-anti-xray")) try {
            AntiXrayClientDebugger.initialise();
        } catch (NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError error) {
            Debug.logger.error("AntiXray found but cannot be loaded.", error);
        }
    }
}
