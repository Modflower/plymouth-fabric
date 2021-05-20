package net.kjp12.plymouth.tracker;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kjp12.plymouth.common.InjectableInteractionManager;
import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.PlymouthNoOP;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Tracker command to inspect, lookup, rollback and restore changes done to the world over time.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class TrackerCommand {
    public static final Predicate<ServerCommandSource>
            REQUIRE_INSPECT_PERMISSION = Permissions.require("plymouth.admin.tracker.inspect", 3),
            REQUIRE_LOOKUP_PERMISSION = Permissions.require("plymouth.admin.tracker.lookup", 3),
            REQUIRE_ROLLBACK_PERMISSION = Permissions.require("plymouth.admin.tracker.rollback", 3);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        // We cannot setup commands if the database is unavailable.
        if (DatabaseHelper.database instanceof PlymouthNoOP) return;

        var p$tracker = literal("tracker");

        // Note: `plymouth tracker` must be initialised before `pt` as `pl` comes first.
        p$tracker // Because for some reason, Brigadier doesn't execute directly after fork, we're forced to initialise the same exact command twice. A bit inconvenient...
                .then(literal("i").requires(REQUIRE_INSPECT_PERMISSION).executes(TrackerCommand::onInspect))
                .then(literal("inspect").requires(REQUIRE_INSPECT_PERMISSION).executes(TrackerCommand::onInspect));

        // TODO: Brigadier needs a better forking system to allow this to work with more specifics.
        //  Hachimitsu Commander maybe able to give this a more rich command experience in the future.
        // var p$lookup = literal("l").requires(REQUIRE_LOOKUP_PERMISSION).then(argument("players", players()).executes(TrackerCommand::onLookup)).build();
        // p$tracker.then(p$lookup).then(literal("lookup").redirect(p$lookup));

        // TODO: A better forking system to allow rollback and restore to be specific.
        //  Hachimitsu Commander maybe able to take place of this in the future.
        // var p$rollback = ;
        // var p$restore = ;

        var b$tracker = p$tracker.build();
        dispatcher.register(literal("plymouth").then(b$tracker));
        // TODO: Config to turn the following two off in case of conflict.
        dispatcher.register(literal("pt").redirect(b$tracker));
        dispatcher.register(literal("tracker").redirect(b$tracker));
    }

    private static int onInspect(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = source.getPlayer();
        var iim = (InjectableInteractionManager) player.interactionManager;
        if (iim.getManager() != null) {
            iim.setManager(null);
            source.sendFeedback(Tracker.INSPECT_END, false);
        } else {
            iim.setManager(new TrackerInspectionManagerInjection());
            source.sendFeedback(Tracker.INSPECT_START, false);
        }
        return Command.SINGLE_SUCCESS;
    }

    // private static int onLookup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    //     var source = ctx.getSource();
    //     // DatabaseHelper.database
    //     getPlayers(ctx, "players");
    //     return 0;
    // }
}
