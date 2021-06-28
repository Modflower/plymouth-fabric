package net.kjp12.plymouth.locking;

import net.kjp12.plymouth.locking.handler.IPermissionHandler;
import org.jetbrains.annotations.Nullable;

public interface ILockable {
    @Nullable
    IPermissionHandler plymouth$getPermissionHandler();

    void plymouth$setPermissionHandler(@Nullable IPermissionHandler handler);

    default boolean plymouth$isOwned() {
        return plymouth$getPermissionHandler() != null;
    }
}
