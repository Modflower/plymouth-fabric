package gay.ampflower.plymouth.debug;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import gay.ampflower.plymouth.anti_xray.IShadowChunk;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public class Debug implements ModInitializer {
    public static final Identifier
            debugAntiXraySet = new Identifier("plymouth-debug", "set"),
            debugAntiXrayUpdate = new Identifier("plymouth-debug", "update"),
            debugAntiXrayTest = new Identifier("plymouth-debug", "test"),
            debugAntiXrayMask = new Identifier("plymouth-debug", "mask");
    public static final Set<MinecraftServer> servers = new HashSet<>();
    public static final Set<ServerPlayNetworkHandler> players = new HashSet<>();

    @Override
    public void onInitialize() {
        // The cursedness of these two methods is what makes this extra special.
        ServerLifecycleEvents.SERVER_STARTING.register(servers::add);
        ServerLifecycleEvents.SERVER_STOPPED.register(servers::remove);
        ServerPlayConnectionEvents.JOIN.register((player, packetSender, server) -> {
            if (ServerPlayNetworking.canSend(player, debugAntiXrayUpdate)) players.add(player);
        });
        // ServerPlayNetworking.canSend()
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("mdump").executes(ctx -> {
            var player = ctx.getSource().getPlayer();
            int cx = MathHelper.floor(player.getX()) >> 4, cz = MathHelper.floor(player.getZ()) >> 4;
            var masks = ((IShadowChunk) player.world.getChunk(cx, cz)).plymouth$getShadowMasks();
            var packet = new PacketByteBuf(Unpooled.buffer()).writeVarInt(cx).writeVarInt(cz).writeVarInt(masks.length);
            for (var mask : masks) {
                if (mask == null) packet.writeVarInt(0);
                else {
                    packet.writeLongArray(mask.toLongArray());
                }
            }
            ServerPlayNetworking.send(player, debugAntiXrayMask, packet);
            return 1;
        })));
    }

    public static void send(Identifier id, long pos) {
        for (var p : Debug.players) {
            ServerPlayNetworking.send(p.player, id, new PacketByteBuf(Unpooled.copyLong(pos)));
        }
    }
}
