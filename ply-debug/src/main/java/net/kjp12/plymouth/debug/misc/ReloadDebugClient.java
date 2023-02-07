package net.kjp12.plymouth.debug.misc;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.kjp12.plymouth.debug.Fusebox;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ReloadDebugClient {
    public static void initialise() {
        ClientCommandRegistrationCallback.EVENT.register((DISPATCHER, registryAccess) ->
                DISPATCHER.register(literal("pdbc").then(literal("reload").executes(ctx -> {
                    Fusebox.reinit();
                    return 1;
                })))
        );
    }
}
