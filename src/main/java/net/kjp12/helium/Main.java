package net.kjp12.helium;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.arguments.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    private static final Predicate<ServerCommandSource>
            REQUIRE_OP_PLAYER = s -> s.hasPermissionLevel(2) && s.getEntity() instanceof ServerPlayerEntity;

    private static Thread[] currentThreads;
    private static int threadToFail;

    public static void initializeCommands(CommandManager.RegistrationEnvironment env, CommandDispatcher<ServerCommandSource> dispatcher) {
        // Lock
        {
            var add = literal("add").
                    then(argument("players", players()).then(argument("permission", integer(0, 1 | 2 | 4)).executes(s -> {
                        var l = getPlayers(s, "players");
                        return Command.SINGLE_SUCCESS;
                    })).executes(s -> {
                        var p = s.getArgument("permission", int.class);
                        var l = getPlayers(s, "players");
                        for (var i : l) {
                            i.getUuid();
                        }
                        return Command.SINGLE_SUCCESS;
                    }));
            var rm = literal("remove").then(argument("players", players()).executes(s -> {
                var l = getPlayers(s, "players");
                return Command.SINGLE_SUCCESS;
            }));
            var l = dispatcher.register(literal("lock")
                    .requires(s -> s.getEntity() instanceof ServerPlayerEntity)
                    .then(add).then(rm));
        }
        // InvSee
        {
            var e0 = argument("target", player()).executes(s -> {
                assert s.getSource().hasPermissionLevel(2) : String.format("BugCheck: %s: %s -> %s", s, s.getSource(), s.getInput());
                var p = getPlayer(s, "target");
                var sp = s.getSource().getPlayer();
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) -> GenericContainerScreenHandler.createGeneric9x3(i, pi, p.getEnderChestInventory()), p.getDisplayName().shallowCopy().append(" - ").append(new TranslatableText(Blocks.ENDER_CHEST.getTranslationKey()).formatted(Formatting.DARK_PURPLE))));
                return Command.SINGLE_SUCCESS;
            });
            var e1 = literal("ender").then(e0);
            var e2 = literal("e").then(e0);

            var p0 = argument("target", player()).executes(s -> {
                assert s.getSource().hasPermissionLevel(2) : String.format("BugCheck: %s: %s -> %s", s, s.getSource(), s.getInput());
                var p = getPlayer(s, "target");
                var sp = s.getSource().getPlayer();
                if (sp.equals(p)) {
                    sp.sendSystemMessage(new LiteralText("Did you mean: ").formatted(Formatting.ITALIC, Formatting.RED).append(new KeybindText("key.inventory").formatted(Formatting.AQUA)).append("?"), Util.NIL_UUID);
                } else {
                    // TODO: Replace with a better screen handler that accounts for hidden player inventory.
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, i, pi, p.inventory, 4) {
                        @Override
                        public boolean canUse(PlayerEntity player) {
                            return true;
                        }
                    }, p.getDisplayName()));
                }
                return Command.SINGLE_SUCCESS;
            });
            var p1 = literal("player").then(p0);
            var p2 = literal("p").then(p0);

            var i1 = dispatcher.register(literal("inventory").requires(REQUIRE_OP_PLAYER)
                    .then(e1).then(e2).then(p0).then(p1).then(p2));
            var i2 = dispatcher.register(literal("invsee").requires(REQUIRE_OP_PLAYER).redirect(i1));
            var i3 = dispatcher.register(literal("endsee").requires(REQUIRE_OP_PLAYER).then(e0));
        }
        // Invisible
        {

        }
    }

    @Override
    public void onInitialize() {
        Helium.LOGGER.info(Helium.ANONYMOUS_BLOCK_UUID);
    }
}
