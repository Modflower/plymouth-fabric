package gay.ampflower.helium.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import gay.ampflower.helium.Helium;
import gay.ampflower.helium.HeliumEarlyRiser;
import net.minecraft.SharedConstants;
import net.minecraft.item.FilledMapItem;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Predicate;

import static gay.ampflower.helium.Helium.httpClient;
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
        var m0 = argument("overwrite", IntegerArgumentType.integer()).then(argument("uri", StringArgumentType.string()).requires(REQUIRE_MAPPING_PERMISSION).executes(ctx -> {
                    assert REQUIRE_MAPPING_PERMISSION.test(ctx.getSource()) : String.format("BugCheck: %s: %s -> %s", ctx, ctx.getSource(), ctx.getInput());
                    var uri = StringArgumentType.getString(ctx, "uri");
                    var map = IntegerArgumentType.getInteger(ctx, "overwrite");
                    var uriLinked = new LiteralText(uri).styled(s -> s.withFormatting(Formatting.BLUE, Formatting.UNDERLINE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uri)));
                    ctx.getSource().sendFeedback(new LiteralText("Downloading from ").append(uriLinked).append(" for Map ").append(Integer.toString(map)), true);
                    // Asynchronous access moment
                    // We need to operate on the overworld due to messing with map states.
                    httpClient.sendAsync(HttpRequest.newBuilder(URI.create(uri)).GET().build(), HttpResponse.BodyHandlers.ofInputStream()).whenCompleteAsync((res, t) -> {
                                var source = ctx.getSource();
                                if (t == null) {
                                    if (res.statusCode() == 200) {
                                        try (var body = res.body()) {
                                            var server = ctx.getSource().getMinecraftServer();
                                            var overworld = server.getOverworld();
                                            var mapState = overworld.getMapState(FilledMapItem.getMapName(map));
                                            // note: data nesting can be blamed on by Mojang, who thought this was a good idea to begin with.
                                            var mapCompound = Helium.readTag(body, server.getDataFixer(), SharedConstants.getGameVersion().getWorldVersion()).getCompound("data");
                                            assert mapState != null;
                                            if (!mapCompound.contains("dimension"))
                                                mapCompound.putString("dimension", overworld.getRegistryKey().getValue().toString());
                                            mapState.fromTag(mapCompound);
                                            mapState.markDirty(0, 0);
                                            mapState.markDirty(127, 127);
                                            source.sendFeedback(new LiteralText("Deployed ").append(uriLinked).append(" to map " + map), true);
                                        } catch (IOException ioe) {
                                            HeliumEarlyRiser.LOGGER.error("[IO Failure] Failed to parse {} for map {}", uri, map, ioe);
                                            source.sendError(new LiteralText("Failed to parse ").append(uriLinked).append(": ").append(new LiteralText(ioe.getLocalizedMessage())));
                                            source.sendError(Helium.SEE_LOGS);
                                        } catch (RuntimeException re) {
                                            HeliumEarlyRiser.LOGGER.error("[Parsing Failure] Failed to parse {} for map {}", uri, map, re);
                                            source.sendError(new LiteralText("Failed to parse ").append(uriLinked).append(": ").append(new LiteralText(re.getLocalizedMessage())));
                                            source.sendError(Helium.SEE_LOGS);
                                        }
                                    } else {
                                        HeliumEarlyRiser.LOGGER.warn("[Server Failure] Failed to download {} for map {}; following dump below.", uri, map);
                                        try (var body = res.body()) {
                                            body.transferTo(System.out);
                                        } catch (IOException ioe) {
                                            HeliumEarlyRiser.LOGGER.warn("[IO Failure] Failed to pipe {}", uri, ioe);
                                        }
                                        source.sendError(new LiteralText("Failed to fetch").append(uriLinked).append("."));
                                        source.sendError(Helium.SEE_LOGS);
                                    }
                                } else {
                                    HeliumEarlyRiser.LOGGER.error("[Client Failure] Failed to download {} for map {}", uri, map, t);
                                    source.sendError(new LiteralText("Failed to download ").append(uriLinked).append(": ").append(new LiteralText(t.getLocalizedMessage())));
                                    source.sendError(Helium.SEE_LOGS);
                                }
                            }, // We're using the server as a thread pool as we need to be synced against the server itself.
                            ctx.getSource().getMinecraftServer());
                    return Command.SINGLE_SUCCESS;
                })
        );
        dispatcher.register(literal("overwrite").then(literal("map").then(m0)));
    }
}