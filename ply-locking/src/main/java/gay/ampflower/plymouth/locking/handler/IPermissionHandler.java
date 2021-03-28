package gay.ampflower.plymouth.locking.handler;

import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.locking.Locking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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

    boolean allowRead(UUID uuid);

    boolean allowWrite(UUID uuid);

    boolean allowDelete(UUID uuid);

    boolean allowPermissions(UUID uuid);

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
        return Locking.LOCKING_BYPASS_READ_PERMISSION.test(source) || allowRead(source.getEntity());
    }

    default boolean allowWrite(ServerCommandSource source) {
        return Locking.LOCKING_BYPASS_WRITE_PERMISSION.test(source) || allowWrite(source.getEntity());
    }

    default boolean allowDelete(ServerCommandSource source) {
        return Locking.LOCKING_BYPASS_DELETE_PERMISSION.test(source) || allowDelete(source.getEntity());
    }

    default boolean allowPermissions(ServerCommandSource source) {
        return Locking.LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(source) || allowPermissions(source.getEntity());
    }

    default void fromTag(CompoundTag tag) {
        setOwner(tag.getUuid("owner"));
        if (tag.contains("group", 8)) {
            setGroup(tag.getString("group"));
        }
    }

    default void toTag(CompoundTag tag) {
        tag.putUuid("owner", getOwner());
        var group = getGroup();
        if (group != null) tag.putString("group", getGroup());
    }

    void dumpLock(ServerCommandSource to);
}
