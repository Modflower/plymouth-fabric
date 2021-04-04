package gay.ampflower.plymouth.debug;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import gay.ampflower.plymouth.debug.anti_xray.AntiXrayDebugger;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The primary initializer for the debug server.
 *
 * @author Ampflower
 * @since 0.0.0
 */
public class Debug implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("Plymouth: Debug");

    @Override
    public void onInitialize() {
        var loader = FabricLoader.getInstance();
        if (loader.isModLoaded("plymouth-anti-xray")) try {
            AntiXrayDebugger.initialise();
        } catch (NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError error) {
            logger.error("AntiXray found but cannot be loaded.", error);
        }
    }

    public static void send(Identifier id, long pos) {
        for (var p : AntiXrayDebugger.players) {
            ServerPlayNetworking.send(p.player, id, new PacketByteBuf(Unpooled.copyLong(pos)));
        }
    }
}
