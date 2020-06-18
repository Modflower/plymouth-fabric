package gay.ampflower.helium.helpers;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;
import java.util.UUID;

public interface IShadowBlockEntity {
    UUID helium$getOwner();

    void helium$setOwner(UUID uuid);

    default boolean helium$isOwner(UUID uuid) {
        return Objects.equals(uuid, helium$getOwner());
    }

    default boolean helium$canOpenBlock(PlayerEntity player) {
        return helium$getOwner() == null || player.hasPermissionLevel(2) || helium$canOpenBlock(player.getUuid());
    }

    default boolean helium$canModifyBlock(PlayerEntity player) {
        return helium$getOwner() == null || player.hasPermissionLevel(2) || helium$canModifyBlock(player.getUuid());
    }

    default boolean helium$canBreakBlock(PlayerEntity player) {
        return helium$getOwner() == null || player.hasPermissionLevel(2) || helium$canBreakBlock(player.getUuid());
    }

    // Note: This doesn't apply to doors.
    // Note: This however does apply to beds.
    boolean helium$canOpenBlock(UUID uuid);

    boolean helium$canModifyBlock(UUID uuid);

    boolean helium$canBreakBlock(UUID uuid);
}
