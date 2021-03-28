package net.kjp12.plymouth.locking;

import net.kjp12.plymouth.locking.handler.BasicPermissionHandler;
import net.kjp12.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public interface ILockable {
    @Nullable
    IPermissionHandler plymouth$getPermissionHandler();

    void plymouth$setPermissionHandler(@Nullable IPermissionHandler handler);

    default boolean plymouth$isOwned() {
        return plymouth$getPermissionHandler() != null;
    }

    @Deprecated(forRemoval = true)
    default UUID helium$getOwner() {
        var h = plymouth$getPermissionHandler();
        return h == null ? null : h.getOwner();
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

    @Deprecated(forRemoval = true)
    default boolean helium$isOwner(UUID uuid) {
        return Objects.equals(uuid, helium$getOwner());
    }

    // Note: This doesn't apply to doors.
    // Note: This however does apply to beds.
    @Deprecated(forRemoval = true)
    default boolean helium$canOpenBlock(PlayerEntity player) {
        var h = plymouth$getPermissionHandler();
        return h == null || h.allowRead(player.getCommandSource());
    }
}
