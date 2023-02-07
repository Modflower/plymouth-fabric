package gay.ampflower.plymouth.debug.misc;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import gay.ampflower.plymouth.debug.Fusebox;

import java.util.ArrayList;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ReloadDebugClient {

    private static final List<Runnable> reloadables = new ArrayList<>();

    public static void initialise() {
        ClientCommandRegistrationCallback.EVENT.register((DISPATCHER, registryAccess) ->
                DISPATCHER.register(literal("pdbc").then(literal("reload").executes(ctx -> {
                    Fusebox.reinit();
                    for (final var reloadable : reloadables) reloadable.run();
                    return 1;
                })))
        );
    }

    public static void addReloadable(Runnable runnable) {
        reloadables.add(runnable);
    }
}
