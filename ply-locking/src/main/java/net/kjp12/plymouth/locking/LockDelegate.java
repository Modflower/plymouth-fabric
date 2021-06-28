package net.kjp12.plymouth.locking;// Created 2021-26-06T07:05:14

import net.kjp12.plymouth.locking.handler.BasicPermissionHandler;
import net.kjp12.plymouth.locking.handler.IPermissionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author KJP12
 * @since ${version}
 **/
public record LockDelegate(int effective, List<ILockable> containers) implements ILockable {
    @Override
    public @Nullable IPermissionHandler plymouth$getPermissionHandler() {
        throw new UnsupportedOperationException("Cannot return list view.");
    }

    @Override
    public void plymouth$setPermissionHandler(@Nullable IPermissionHandler handler) {
        throw new UnsupportedOperationException("Cannot broadcast handler.");
    }

    @Override
    public boolean plymouth$isOwned() {
        return (effective & Locking.OWNED) != 0;
    }

    public boolean canOpen() {
        return (effective & Locking.READ_PERMISSION) != 0;
    }

    public boolean isOwner() {
        return (effective & Locking.OWNER) != 0;
    }

    public UUID getOwner() {
        return plymouth$isOwned() ? containers.stream().map(ILockable::plymouth$getPermissionHandler).filter(Objects::nonNull).map(IPermissionHandler::getOwner).findFirst().orElse(null) : null;
    }

    public void unclaim() {
        for (var container : containers) container.plymouth$setPermissionHandler(null);
    }

    public void claim(UUID uuid) {
        for (var container : containers) container.plymouth$setPermissionHandler(new BasicPermissionHandler(uuid));
    }
}
