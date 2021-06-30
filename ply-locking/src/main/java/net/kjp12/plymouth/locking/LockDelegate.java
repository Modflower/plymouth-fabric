package net.kjp12.plymouth.locking;// Created 2021-26-06T07:05:14

import net.kjp12.plymouth.locking.handler.AdvancedPermissionHandler;
import net.kjp12.plymouth.locking.handler.BasicPermissionHandler;
import net.kjp12.plymouth.locking.handler.IAdvancedPermissionHandler;
import net.kjp12.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        if (!plymouth$isOwned()) return null;
        for (var container : containers) {
            var handler = container.plymouth$getPermissionHandler();
            if (handler != null) return handler.getOwner();
        }
        throw new IllegalStateException("Owned but no owners found. Old delegate?");
    }

    public void unclaim() {
        for (var container : containers) container.plymouth$setPermissionHandler(null);
    }

    public void compute(UnaryOperator<IPermissionHandler> operator) {
        for (var container : containers)
            container.plymouth$setPermissionHandler(operator.apply(container.plymouth$getPermissionHandler()));
    }

    public void claimAdvanced(UUID uuid) {
        for (var container : containers) container.plymouth$setPermissionHandler(new AdvancedPermissionHandler(uuid));
    }

    public void claim(UUID uuid) {
        for (var container : containers) container.plymouth$setPermissionHandler(new BasicPermissionHandler(uuid));
    }

    public void modifyPlayers(Collection<? extends PlayerEntity> players, short permissions) {
        for (var container : containers) {
            var handler = container.plymouth$getPermissionHandler();
            if (handler instanceof IAdvancedPermissionHandler aph) {
                aph.modifyPlayers(players, permissions);
            } else {
                var aph = new AdvancedPermissionHandler(handler);
                container.plymouth$setPermissionHandler(aph);
                aph.modifyPlayers(players, permissions);
            }
        }
    }

    public void removePlayers(Collection<? extends PlayerEntity> players) {
        for (var container : containers) {
            if (container.plymouth$getPermissionHandler() instanceof IAdvancedPermissionHandler aph) {
                aph.removePlayers(players);
            }
            // There's nothing to do on basic.
        }
    }

    public void modifyPermissions(int permissions) {
        for (var container : containers) container.plymouth$getPermissionHandler().modifyPermissions(permissions);
    }

    public void markDirty() {
        for (var container : containers) if (container instanceof BlockEntity block) block.markDirty();
    }

    public Block getBlock() {
        for (var container : containers)
            if (container instanceof BlockEntity block) return block.getCachedState().getBlock();
        return Blocks.AIR;
    }
}
