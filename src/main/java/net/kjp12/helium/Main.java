package net.kjp12.helium;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

public class Main implements ModInitializer {
    public static final Predicate<ServerCommandSource>
            REQUIRE_MUTE_PERMISSION = Permissions.require("helium.admin.moderation.mute", 3),
            REQUIRE_LOCKING_PERMISSION = Permissions.require("helium.locking.lock", true);

// Invisible
// Mute (atm. will just inject into the player file a long-timestamp of when they'll be unmuted, or perhaps just a boolean of if they're muted or not.)
// TODO: Signage and book-write blocking? *handle by permissions?*
// Unmute (atm. will just inject into the player file to mute/unmute them.)

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Runs permissions checks using MinecraftServer as the command source, preinitializing any permission nodes.
            var source = server.getCommandSource();
            REQUIRE_LOCKING_PERMISSION.test(source);
        });
    }
}
