package net.kjp12.plymouth.locking.handler;// Created 2021-03-23T03:49:40

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Permission handler designed for POSIX-like usage. Permission bits are Read, Write, Delete and Permissions, or RWDP.
 *
 * @author KJP12
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
        this(handler.getOwner(), handler.getGroup());
        if (handler instanceof BasicPermissionHandler) {
            this.permissions = ((BasicPermissionHandler) handler).permissions;
        }
    }

    public BasicPermissionHandler(UUID owner) {
        this.owner = owner;
    }

    public BasicPermissionHandler(UUID owner, String group) {
        this.owner = owner;
        this.group = group;
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
    public boolean allowRead(UUID uuid) {
        return owner.equals(uuid);
    }

    @Override
    public boolean allowWrite(UUID uuid) {
        return owner.equals(uuid);
    }

    @Override
    public boolean allowDelete(UUID uuid) {
        return owner.equals(uuid);
    }

    @Override
    public boolean allowPermissions(UUID uuid) {
        return owner.equals(uuid);
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
        to.sendFeedback(new TranslatableText("plymouth.locking.dump.basic", owner, group, permissions), false);
    }
}
