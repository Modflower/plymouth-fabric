package gay.ampflower.plymouth.debug;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import gay.ampflower.plymouth.debug.anti_xray.AntiXrayDebugger;
import net.minecraft.SharedConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;

/**
 * The primary initializer for the debug server.
 *
 * @author Ampflower
 * @since 0.0.0
 */
public class Debug implements ModInitializer {
    public static final Logger logger = LoggerFactory.getLogger("Plymouth: Debug");
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final MethodHandles.Lookup self = MethodHandles.lookup();

    @Override
    public void onInitialize() {
        SharedConstants.isDevelopment = Fusebox.isEnabled("minecraft.development");
        var loader = FabricLoader.getInstance();
        if (loader.isModLoaded("plymouth-anti-xray")) {
            if (Fusebox.isEnabled("antiXrayDebug")) {
                tryOrLog(AntiXrayDebugger::initialise, "AntiXray found but cannot be loaded.");
            } else {
                logger.info("Anti-Xray debugging is disabled. Add to the config, `antiXrayDebug=true`, if you wish to debug the anti-xray engine.");
            }
        }
    }

    public static void tryOrLog(Runnable callable, String message) {
        try {
            callable.run();
        } catch (LinkageError error) {
            logger.error(message, error);
        }
    }

    public static void send(Identifier id, long pos) {
        for (var p : AntiXrayDebugger.players) {
            ServerPlayNetworking.send(p.player, id, new PacketByteBuf(Unpooled.copyLong(pos)));
        }
    }

    public static void printRichStack() {
        logger.info("Stacktrace at head\n{}", (String) walker.walk(stream -> stream.map(Debug::mux).collect(Collectors.joining("\n"))));
    }

    private static String mux(StackWalker.StackFrame frame) {
        // try {
        //     var method = frame.getDeclaringClass().getDeclaredMethod(frame.getMethodName(), frame.getMethodType().parameterArray());
        //     method.getAnnotation()
        // } catch (NoSuchMethodException | SecurityException exception) {
        //     ;
        // }
        // self.findVirtual(frame.getDeclaringClass(), frame.getMethodName(), frame.getMethodType()).
        // MethodHandles.reflectAs()
        // frame.getMethodType()
        return frame.toString();
    }
}
