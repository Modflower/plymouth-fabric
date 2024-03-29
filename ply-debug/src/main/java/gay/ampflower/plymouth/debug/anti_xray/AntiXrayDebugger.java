package gay.ampflower.plymouth.debug.anti_xray;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import gay.ampflower.plymouth.antixray.ShadowChunk;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public class AntiXrayDebugger {
    public static final Identifier
            debugAntiXraySet = new Identifier("plymouth-debug", "set"),
            debugAntiXrayUpdate = new Identifier("plymouth-debug", "update"),
            debugAntiXrayTest = new Identifier("plymouth-debug", "test"),
            debugAntiXrayMask = new Identifier("plymouth-debug", "mask");
    public static final Set<ServerPlayNetworkHandler> players = new HashSet<>();

    public static boolean canSendDebugInformation(ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity player && players.contains(player.networkHandler);
    }

    public static void initialise() {
        ServerPlayConnectionEvents.JOIN.register((player, packetSender, server) -> {
            if (ServerPlayNetworking.canSend(player, AntiXrayDebugger.debugAntiXrayUpdate))
                AntiXrayDebugger.players.add(player);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("mdump").requires(AntiXrayDebugger::canSendDebugInformation).executes(ctx -> {
            var player = ctx.getSource().getPlayerOrThrow();
            int cx = MathHelper.floor(player.getX()) >> 4, cz = MathHelper.floor(player.getZ()) >> 4;
            var mask = ((ShadowChunk) player.world.getChunk(cx, cz)).plymouth$getShadowMask();
            var packet = new PacketByteBuf(Unpooled.buffer()).writeVarInt(cx).writeVarInt(cz).writeLongArray(mask.toLongArray());
            ServerPlayNetworking.send(player, AntiXrayDebugger.debugAntiXrayMask, packet);
            return 1;
        })));
    }
}
