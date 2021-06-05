package gay.ampflower.plymouth.tracker;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import gay.ampflower.hachimitsu.utilities.StringSpliterator;
import gay.ampflower.plymouth.common.InjectableInteractionManager;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.PlymouthNoOP;
import gay.ampflower.plymouth.database.records.LookupRecord;
import gay.ampflower.plymouth.database.records.PlymouthRecord;
import gay.ampflower.plymouth.database.records.RecordType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Tracker command to inspect, lookup, rollback and restore changes done to the world over time.
 *
 * @author Ampflower
 * @since 0.0.0
 **/
public class TrackerCommand {
    public static final Predicate<ServerCommandSource>
            REQUIRE_INSPECT_PERMISSION = Permissions.require("plymouth.admin.tracker.inspect", 3),
            REQUIRE_LOOKUP_PERMISSION = Permissions.require("plymouth.admin.tracker.lookup", 3),
            REQUIRE_ROLLBACK_PERMISSION = Permissions.require("plymouth.admin.tracker.rollback", 3);

    private static final DynamicCommandExceptionType
            PARSER_INVALID = new DynamicCommandExceptionType(i -> new TranslatableText("commands.plymouth.tracker.invalid", i));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        // We cannot setup commands if the database is unavailable.
        if (DatabaseHelper.database instanceof PlymouthNoOP) return;

        var p$tracker = literal("tracker");

        // Note: `plymouth tracker` must be initialised before `pt` as `pl` comes first.
        p$tracker // Because for some reason, Brigadier doesn't execute directly after fork, we're forced to initialise the same exact command twice. A bit inconvenient...
                .then(literal("i").requires(REQUIRE_INSPECT_PERMISSION).executes(TrackerCommand::onInspect))
                .then(literal("inspect").requires(REQUIRE_INSPECT_PERMISSION).executes(TrackerCommand::onInspect));

        var p$lookup = literal("l").requires(REQUIRE_LOOKUP_PERMISSION)
                .then(argument("query", StringArgumentType.greedyString()).suggests(TrackerCommand::lookupSuggestions).executes(TrackerCommand::onLookup)).build();
        p$tracker.then(p$lookup).then(literal("lookup").redirect(p$lookup));

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

    private static StringBuilder mkSb(String str) {
        var sb = new StringBuilder(str);
        if (sb.length() != 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
        return sb;
    }

    private static BlockPos mkPos(StringSpliterator itr) {
        return new BlockPos(itr.nextInt(), itr.nextInt(), itr.nextInt());
    }

    // TODO: https://mvnrepository.com/artifact/org.ow2.asm/asm
    private static CompletableFuture<Suggestions> lookupSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        var parser = new StringSpliterator(builder.getRemaining());
        int f = 0, s = 0, c = 0;

        final int u = 0x1, t = 0x2, a = 0x4, r = 0x8;
        StringBuilder psb;

        primary:
        while (parser.hasNext()) {
            parser.next();
            switch (s) {
                case 0:
                    switch (parser.currentHashScreamingSnake()) {
                        case 2135: // BY u
                            if (parser.contentEquals("by", true)) {
                                f |= u;
                                s = 1;
                            }
                            break;
                        case 2575053: // TIME s e
                            if (parser.contentEquals("time", true)) {
                                f |= t;
                                s = 2;
                            }
                            break;
                        case 2099: // AT x y z
                            if (parser.contentEquals("at", true)) {
                                f |= a;
                                s = 3;
                            }
                            break;
                        case 2017421: // AREA x y z x y z
                            if (parser.contentEquals("area", true)) {
                                f |= a;
                                s = 4;
                            }
                            break;
                        case -1885249390: // RADIUS r
                            if (parser.contentEquals("radius", true)) {
                                f |= a;
                                s = 5;
                            }
                            break;
                        case 63294573: // BLOCK
                            if (parser.contentEquals("block", true)) {
                                f |= r;
                            }
                            break;
                        case 64920148: // DEATH
                            if (parser.contentEquals("death", true)) {
                                f |= r;
                            }
                            break;
                        case 765995324: // INVENTORY
                            if (parser.contentEquals("inventory", true)) {
                                f |= r;
                            }
                            break;
                        default:
                            if (parser.hasNext()) {
                                // Cannot continue, throw.
                                throw PARSER_INVALID.create(parser.current());
                            } else {
                                // We'll back track this to figure out
                                parser.backtrack();
                                break primary;
                            }
                    }
                    break;
                case 1:
                case 5:
                    s = c = 0;
                    break;
                case 2:
                    if (++c == 2) s = c = 0;
                    break;
                case 3:
                    if (++c == 3) s = c = 0;
                    break;
                case 4:
                    if (++c == 6) s = c = 0;
                    break;
                default:
                    throw new AssertionError("State: " + s + ", flags: " + f + ", input: " + builder);
            }
        }
        // TODO: Make the suggestions quote aware.
        switch (s) {
            case 0:
                if (parser.hasNext()) {
                    parser.anchor(16);
                    if ((f & u) == 0) {
                        if (parser.tryTestAnchor("by")) builder.suggest(parser.getAnchor());
                    }
                    if ((f & t) == 0) {
                        if (parser.tryTestAnchor("time")) builder.suggest(parser.getAnchor());
                    }
                    if ((f & a) == 0) {
                        if (parser.tryTestAnchor("at")) builder.suggest(parser.getAnchor());
                        if (parser.tryTestAnchor("area")) builder.suggest(parser.getAnchor());
                        if (parser.tryTestAnchor("radius")) builder.suggest(parser.getAnchor());
                    }
                    if ((f & r) == 0) {
                        if (parser.tryTestAnchor("block")) builder.suggest(parser.getAnchor());
                        if (parser.tryTestAnchor("death")) builder.suggest(parser.getAnchor());
                        if (parser.tryTestAnchor("inventory")) builder.suggest(parser.getAnchor());
                    }
                } else {
                    psb = mkSb(builder.getRemaining());
                    var len = psb.length();
                    if ((f & u) == 0) {
                        builder.suggest(psb.replace(len, psb.length(), "by").toString());
                    }
                    if ((f & t) == 0) {
                        builder.suggest(psb.replace(len, psb.length(), "time").toString());
                    }
                    if ((f & a) == 0) {
                        builder.suggest(psb.replace(len, psb.length(), "at").toString());
                        builder.suggest(psb.replace(len, psb.length(), "area").toString());
                        builder.suggest(psb.replace(len, psb.length(), "radius").toString());
                    }
                    if ((f & r) == 0) {
                        builder.suggest(psb.replace(len, psb.length(), "block").toString());
                        builder.suggest(psb.replace(len, psb.length(), "death").toString());
                        builder.suggest(psb.replace(len, psb.length(), "inventory").toString());
                    }
                }
                break;
            case 1:
                psb = mkSb(builder.getRemaining());
                // for now, suggest the runner's uuid
                builder.suggest(psb.append(context.getSource().getEntity().getUuid()).toString());
                // TODO: name, uuid, identifiers
                break;
            case 2:
                psb = mkSb(builder.getRemaining());
                DateTimeFormatter.ISO_INSTANT.formatTo(Instant.now(), psb);
                builder.suggest(psb.toString());
                break;
            case 3:
            case 4:
                var source = context.getSource();
                var entity = source.getEntity();
                var cast = entity != null ? entity.raycast(5, 0, false) : null;
                var pos = cast instanceof BlockHitResult ? ((BlockHitResult) cast).getBlockPos() :
                        entity != null ? entity.getBlockPos() : new BlockPos(source.getEntityAnchor().positionAt(source));
                psb = mkSb(builder.getRemaining());
                switch (c % 3) {
                    case 0:
                        builder.suggest(psb.append(pos.getX()).toString());
                        break;
                    case 1:
                        builder.suggest(psb.append(pos.getY()).toString());
                        break;
                    case 2:
                        builder.suggest(psb.append(pos.getZ()).toString());
                        break;
                }
                break;
            case 5:
                psb = mkSb(builder.getRemaining());
                var len = psb.length();
                builder.suggest(psb.replace(len, psb.length(), "8").toString());
                builder.suggest(psb.replace(len, psb.length(), "16").toString());
                builder.suggest(psb.replace(len, psb.length(), "32").toString());
                builder.suggest(psb.replace(len, psb.length(), "64").toString());
                break;
        }
        return builder.buildFuture();
    }

    private static int onLookup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        int f = 0, s = 0, page = 0;
        RecordType type = RecordType.LOOKUP;
        BlockPos minPos = null, maxPos = null;
        UUID causeUuid = null;
        Instant minTime = null, maxTime = null;
        var parser = new StringSpliterator(StringArgumentType.getString(ctx, "query"));
        final int u = 0x1, t = 0x2, a = 0x4, r = 0x8;

        while (parser.hasNext()) {
            switch (parser.nextHashScreamingSnake()) {
                case 2135: // BY u
                    if ((s & u) == 0 && parser.contentEquals("by", true)) {
                        s |= u;
                        f |= LookupRecord.FLAG_BY;
                        // debate on if this would be more ideal
                        // TODO: a more native fromString method?
                        causeUuid = UUID.fromString(parser.nextString());
                    }
                    break;
                case 2575053: // TIME s e
                    if ((s & t) == 0 && parser.contentEquals("time", true)) {
                        s |= t;
                        f |= LookupRecord.FLAG_TIME;
                        minTime = Instant.parse(parser.nextString());
                        maxTime = Instant.parse(parser.nextString());
                    }
                    break;
                case 2099: // AT x y z
                    if ((s & a) == 0 && parser.contentEquals("at", true)) {
                        s |= a;
                        f |= LookupRecord.FLAG_AT;
                        minPos = mkPos(parser);
                    }
                    break;
                case 2017421: // AREA x y z x y z
                    if ((s & a) == 0 && parser.contentEquals("area", true)) {
                        s |= a;
                        f |= LookupRecord.FLAG_AREA;
                        minPos = mkPos(parser);
                        maxPos = mkPos(parser);
                    }
                    break;
                case -1885249390: // RADIUS r
                    if ((s & a) == 0 && parser.contentEquals("radius", true)) {
                        s |= a;
                        f |= LookupRecord.FLAG_AREA;
                        int $0 = parser.nextInt(), $1 = -$0;
                        var pos = new BlockPos(source.getPosition());
                        minPos = pos.add($1, $1, $1);
                        maxPos = pos.add($0, $0, $0);
                    }
                    break;
                case 63294573: // BLOCK
                    if ((s & r) == 0 && parser.contentEquals("block", true)) {
                        s |= r;
                        type = RecordType.BLOCK;
                    }
                    break;
                case 64920148: // DEATH
                    if ((s & r) == 0 && parser.contentEquals("death", true)) {
                        s |= r;
                        type = RecordType.DEATH;
                    }
                case 765995324: // INVENTORY
                    if ((s & r) == 0 && parser.contentEquals("inventory", true)) {
                        s |= r;
                        type = RecordType.INVENTORY;
                    }
            }
        }

        return lookup(ctx, type, source.getWorld(), minPos, maxPos, causeUuid, minTime, maxTime, page, f);
    }

    @Cmd(suggestions = "^lookupSuggestions(*CommandContext;*SuggestionsBuilder;)*CompletableFuture;", bridge = "^lookup(*CommandContext;)I")
    private static int lookup(CommandContext<ServerCommandSource> ctx, @Param RecordType type, ServerWorld world, BlockPos minPosition, BlockPos maxPosition,
                              UUID causeUuid, Instant minTime, Instant maxTime, int page, int flags) {
        var future = new CompletableFuture<List<PlymouthRecord>>();
        var lookup = new LookupRecord(future, world, minPosition, maxPosition, causeUuid, minTime, maxTime, page, type.bits | flags);
        DatabaseHelper.database.queue(lookup);
        final var player = ctx.getSource();
        future.thenAcceptAsync(l -> {
            try {
                if ((lookup.flags() & LookupRecord.FLAG_AT) != 0) {
                    for (var r : l) {
                        player.sendFeedback(r.toTextNoPosition(), false);
                    }
                } else {
                    for (var r : l) {
                        player.sendFeedback(r.toText(), false);
                    }
                }
                player.sendFeedback(new TranslatableText("commands.plymouth.tracker.lookup", lookup.toText(), "UTC").formatted(Formatting.DARK_GRAY), false);
            } catch (Throwable t) {
                Tracker.logger.error("aaaa", t);
                throw new Error(t);
            }
        }).exceptionally(t -> {
            Tracker.logger.error("eeee", t);
            player.sendFeedback(new LiteralText(t.getLocalizedMessage()).formatted(Formatting.RED), false);
            return null;
        });
        return 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface Cmd {
        /**
         * <code>^</code> - this class, in place of VZBCSIJFDL
         * <code>&</code> - this instance, in place of VZBCSIJFDL
         * <code>*</code> - any package, in place of L
         */
        String suggestions();

        String bridge();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface Param {
        int flags() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface Flags {
        int value();
    }
}
