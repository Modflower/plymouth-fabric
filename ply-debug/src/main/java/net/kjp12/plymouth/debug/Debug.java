package net.kjp12.plymouth.debug;// Created 2021-03-28T20:11:21

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.kjp12.plymouth.debug.anti_xray.AntiXrayDebugger;
import net.minecraft.SharedConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;

/**
 * The primary initializer for the debug server.
 *
 * @author KJP12
 * @since 0.0.0
 */
public class Debug implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("Plymouth: Debug");
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final MethodHandles.Lookup self = MethodHandles.lookup();

    @Override
    public void onInitialize() {
        SharedConstants.isDevelopment = true;
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
