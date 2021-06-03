package gay.ampflower.plymouth.locking;

import gay.ampflower.plymouth.locking.handler.BasicPermissionHandler;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ILockable {
    @Nullable
    IPermissionHandler plymouth$getPermissionHandler();

    void plymouth$setPermissionHandler(@Nullable IPermissionHandler handler);

    default boolean plymouth$isOwned() {
        return plymouth$getPermissionHandler() != null;
    }

    @Deprecated(forRemoval = true)
    default void helium$setOwner(UUID uuid) {
        if (uuid == null) {
            plymouth$setPermissionHandler(null);
        } else {
            var h = plymouth$getPermissionHandler();
            if (h != null) h.setOwner(uuid);
            else plymouth$setPermissionHandler(new BasicPermissionHandler(uuid));
        }
    }

    // Note: This doesn't apply to doors.
    // Note: This however does apply to beds.
    @Deprecated(forRemoval = true)
    default boolean helium$canOpenBlock(PlayerEntity player) {
        var h = plymouth$getPermissionHandler();
        return h == null || h.allowRead(player.getCommandSource());
    }
}
