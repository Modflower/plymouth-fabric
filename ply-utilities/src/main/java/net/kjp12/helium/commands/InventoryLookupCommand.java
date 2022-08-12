package net.kjp12.helium.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kjp12.helium.Helium;
import net.kjp12.helium.mixins.AccessorPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Player inventory lookup. Does not do offline players, only those that are online at the time.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class InventoryLookupCommand {
    public static final Predicate<ServerCommandSource>
            REQUIRE_INVSEE_PERMISSION = Permissions.require("helium.admin.moderation.invsee", 3),
            REQUIRE_ENDSEE_PERMISSION = Permissions.require("helium.admin.moderation.endsee", 3);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var e0 = argument("target", player())
                .requires(REQUIRE_ENDSEE_PERMISSION).executes(s -> {
                    var p = getPlayer(s, "target");
                    var sp = s.getSource().getPlayerOrThrow();
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) ->
                            GenericContainerScreenHandler.createGeneric9x3(i, pi, p.getEnderChestInventory()),
                            p.getDisplayName().copyContentOnly().append(" - ").append(Helium.ENDER_CHEST)));
                    return Command.SINGLE_SUCCESS;
                });

        var p0 = argument("target", player()).requires(REQUIRE_INVSEE_PERMISSION).executes(s -> {
            var p = getPlayer(s, "target");
            var sp = s.getSource().getPlayerOrThrow();
            if (sp.equals(p)) {
                sp.sendMessage(Helium.DID_YOU_MEAN);
            } else {
                // TODO: Replace with a better screen handler that accounts for hidden player inventory.
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) ->
                        new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, i, pi, ((AccessorPlayerEntity) p).getInventory(), 4) {
                            @Override
                            public boolean canUse(PlayerEntity player) {
                                return true;
                            }
                        }, p.getDisplayName()));
            }
            return Command.SINGLE_SUCCESS;
        });

        var i1 = dispatcher.register(literal("inventory")
                .then(literal("ender").then(e0))
                .then(literal("e").then(e0))
                .then(literal("player").then(p0))
                .then(literal("p").then(p0))
                .then(p0));
        dispatcher.register(literal("inv").redirect(i1));
        dispatcher.register(literal("invsee").redirect(i1));
        dispatcher.register(literal("endsee").then(e0));
    }
}
