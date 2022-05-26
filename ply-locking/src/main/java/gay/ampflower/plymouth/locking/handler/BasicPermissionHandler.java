package gay.ampflower.plymouth.locking.handler;

import gay.ampflower.plymouth.common.TextHelper;
import gay.ampflower.plymouth.locking.Locking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Permission handler designed for POSIX-like usage. Permission bits are Read, Write, Delete and Permissions, or RWDP.
 *
 * @author Ampflower
 * @since 0.0.0
 */
public class BasicPermissionHandler implements IPermissionHandler {
    protected UUID owner;
    protected String group;
    // 3x rwdp
    protected short permissions;

    public BasicPermissionHandler() {
    }

    public BasicPermissionHandler(IPermissionHandler handler) {
        this(handler.getOwner(), handler.getGroup(), handler.getPermissions());
    }

    public BasicPermissionHandler(UUID owner) {
        this(owner, null, (short) Locking.DEFAULT_UMASK);
    }

    public BasicPermissionHandler(UUID owner, String group, short permissions) {
        this.owner = owner;
        this.group = group;
        this.permissions = permissions;
    }

    @Override
    public final @NotNull UUID getOwner() {
        return owner;
    }

    @Override
    public final void setOwner(@NotNull UUID owner) {
        // We're requiring non-null to force stuff to explode.
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public final String getGroup() {
        return group;
    }

    @Override
    public final void setGroup(String group) {
        this.group = group;
    }

    @Override
    public short getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(short permissions) {
        this.permissions = permissions;
    }

    @Override
    public void modifyPermissions(int permissions) {
        this.permissions = (short) ((this.permissions & ~(permissions >>> 16)) | permissions & 0xFFFF);
    }

    @Override
    public void fromTag(NbtCompound tag) {
        IPermissionHandler.super.fromTag(tag);
        // public for legacy handling
        permissions = (short) (tag.getShort("permissions") | tag.getByte("public"));
    }

    @Override
    public void toTag(NbtCompound tag) {
        IPermissionHandler.super.toTag(tag);
        tag.putShort("permissions", permissions);
    }

    @Override
    public void dumpLock(ServerCommandSource to) {
        // Lock owned by, group, and permissions
        to.sendFeedback(TextHelper.translatable("plymouth.locking.dump.basic", owner, group, Locking.toString(permissions), Locking.toString(effectivePermissions(to))), false);
    }
}
