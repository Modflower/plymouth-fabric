package gay.ampflower.helium.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import gay.ampflower.helium.Helium;
import gay.ampflower.helium.mixins.AccessorMapState;
import net.minecraft.SharedConstants;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
public class MappingCommand {
    public static final Predicate<ServerCommandSource>
            REQUIRE_MAPPING_PERMISSION = scs -> scs.hasPermissionLevel(3);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var m0 = argument("id", IntegerArgumentType.integer())
                .then(argument("uri", StringArgumentType.greedyString()).executes(ctx -> {
                    onMapOverwrite(ctx.getSource(), StringArgumentType.getString(ctx, "uri"),
                            IntegerArgumentType.getInteger(ctx, "id"));
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(literal("overwrite").then(literal("map").requires(REQUIRE_MAPPING_PERMISSION).then(m0)));
    }

    private static void onMapOverwrite(ServerCommandSource source, String mapUri, int mapId) {
        var uriLinked = new LiteralText(mapUri).setStyle(
                Helium.LINK.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mapUri)));
        source.sendFeedback(new TranslatableText("plymouth.map.download", uriLinked, mapId), true);
        // Asynchronous access moment
        // We need to operate on the overworld due to messing with map states.
        ForkJoinPool.commonPool().execute(() -> {
            try {
                var connection = new URL(mapUri).openConnection();
                if (connection instanceof HttpURLConnection httpConnection) {
                    httpConnection.setRequestMethod("GET");
                    httpConnection.setInstanceFollowRedirects(true);
                    if (httpConnection.getResponseCode() / 100 != 2) {
                        Helium.logger.error("[Server Failure] Failed to download {} for map {}; following dump below.",
                                mapUri, mapId);
                        source.sendError(new TranslatableText("plymouth.map.fail.download", uriLinked,
                                httpConnection.getResponseMessage()));
                        source.sendError(Helium.SEE_LOGS);
                        try (var body = httpConnection.getErrorStream()) {
                            body.transferTo(System.err);
                        } catch (IOException ioe) {
                            Helium.logger.warn("[IO Failure] Failed to pipe err of {}", mapUri, ioe);
                        }
                        try (var body = httpConnection.getInputStream()) {
                            body.transferTo(System.err);
                        } catch (IOException ioe) {
                            Helium.logger.warn("[IO Failure] Failed to pipe out of {}", mapUri, ioe);
                        }
                        return;
                    }
                } else throw new IllegalArgumentException("Non-HTTP requests not allowed.");
                try (var body = connection.getInputStream()) {
                    // This *must* be submitted to the server and joined,
                    // else we risk both closing the pipe too early,
                    // and possibly corrupting the persistent storage.
                    source.getServer().submitAndJoin(() -> {
                        try {
                            var server = source.getServer();
                            var overworld = server.getOverworld();
                            var mapCompound = Helium.readTag(body, server.getDataFixer(),
                                    SharedConstants.getGameVersion().getSaveVersion().getId()).getCompound("data");
                            var mapName = FilledMapItem.getMapName(mapId);
                            var oldMapState = (MapState & AccessorMapState) overworld.getMapState(mapName);
                            assert oldMapState != null;
                            if (!mapCompound.contains("dimension"))
                                mapCompound.putString("dimension", overworld.getRegistryKey().getValue().toString());
                            var newMapState = (MapState & AccessorMapState) MapState.fromNbt(mapCompound);
                            overworld.putMapState(mapName, newMapState);
                            for (var updateTracker : oldMapState.getUpdateTrackers()) {
                                newMapState.getPlayerSyncData(updateTracker.player);
                            }
                            newMapState.callMarkDirty(0, 0);
                            newMapState.callMarkDirty(127, 127);
                            source.sendFeedback(new TranslatableText("plymouth.map.deploy", uriLinked, mapId), true);
                        } catch (RuntimeException | IOException ioe) {
                            Helium.logger.error("[IO Failure] Failed to parse {} for map {}", mapUri, mapId, ioe);
                            source.sendError(new TranslatableText("plymouth.map.fail.parse", uriLinked,
                                    ioe.getLocalizedMessage()));
                            source.sendError(Helium.SEE_LOGS);
                        }
                    });
                }
            } catch (IOException ioe) {
                Helium.logger.error("[IO Failure] Failed to download {} for map {}", mapUri, mapId, ioe);
                source.sendError(new TranslatableText("plymouth.map.fail.download", uriLinked,
                        ioe.getLocalizedMessage()));
                source.sendError(Helium.SEE_LOGS);
            }
        });
    }
}
