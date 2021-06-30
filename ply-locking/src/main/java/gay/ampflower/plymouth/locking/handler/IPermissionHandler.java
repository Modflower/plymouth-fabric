package gay.ampflower.plymouth.locking.handler;

import gay.ampflower.plymouth.common.UUIDHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static gay.ampflower.plymouth.locking.Locking.*;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public interface IPermissionHandler {
    @NotNull
    UUID getOwner();

    void setOwner(@NotNull UUID owner);

    default boolean isOwner(UUID test) {
        return getOwner().equals(test);
    }

    // TODO: Groups system
    String getGroup();

    void setGroup(String group);

    short getPermissions();

    void setPermissions(short permissions);

    void modifyPermissions(int permissions);

    default boolean allowRead(UUID uuid) {
        return isOwner(uuid);
    }

    default boolean allowWrite(UUID uuid) {
        return isOwner(uuid);
    }

    default boolean allowDelete(UUID uuid) {
        return isOwner(uuid);
    }

    default boolean allowPermissions(UUID uuid) {
        return isOwner(uuid);
    }

    default boolean allowRead(Entity entity) {
        return allowRead(UUIDHelper.getUUID(entity));
    }

    default boolean allowWrite(Entity entity) {
        return allowWrite(UUIDHelper.getUUID(entity));
    }

    default boolean allowDelete(Entity entity) {
        return allowDelete(UUIDHelper.getUUID(entity));
    }

    default boolean allowPermissions(Entity entity) {
        return allowPermissions(UUIDHelper.getUUID(entity));
    }

    default boolean allowRead(ServerCommandSource source) {
        return LOCKING_BYPASS_READ_PERMISSION.test(source) || allowRead(source.getEntity());
    }

    default boolean allowWrite(ServerCommandSource source) {
        return LOCKING_BYPASS_WRITE_PERMISSION.test(source) || allowWrite(source.getEntity());
    }

    default boolean allowDelete(ServerCommandSource source) {
        return LOCKING_BYPASS_DELETE_PERMISSION.test(source) || allowDelete(source.getEntity());
    }

    default boolean allowPermissions(ServerCommandSource source) {
        return LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(source) || allowPermissions(source.getEntity());
    }

    default int effectivePermissions(ServerCommandSource source) {
        if (isOwner(UUIDHelper.getUUID(source.getEntity()))) return -1;
        int permissions = OWNED; // group handling logic
        if (LOCKING_BYPASS_READ_PERMISSION.test(source)) permissions |= READ_PERMISSION | READ_BYPASS;
        if (LOCKING_BYPASS_WRITE_PERMISSION.test(source)) permissions |= WRITE_PERMISSION | WRITE_BYPASS;
        if (LOCKING_BYPASS_DELETE_PERMISSION.test(source)) permissions |= DELETE_PERMISSION | DELETE_BYPASS;
        if (LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(source))
            permissions |= PERMISSIONS_PERMISSION | PERMISSIONS_BYPASS;
        return permissions;
    }

    default boolean hasAnyPermissions(ServerCommandSource source) {
        return isOwner(UUIDHelper.getUUID(source.getEntity())) ||
                LOCKING_BYPASS_READ_PERMISSION.test(source) ||
                LOCKING_BYPASS_WRITE_PERMISSION.test(source) ||
                LOCKING_BYPASS_DELETE_PERMISSION.test(source) ||
                LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(source);
    }

    default void fromTag(NbtCompound tag) {
        setOwner(tag.getUuid("owner"));
        if (tag.contains("group", 8)) {
            setGroup(tag.getString("group"));
        }
    }

    default void toTag(NbtCompound tag) {
        tag.putUuid("owner", getOwner());
        var group = getGroup();
        if (group != null) tag.putString("group", getGroup());
    }

    void dumpLock(ServerCommandSource to);
}
