package net.kjp12.sodium.helpers;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;
import java.util.UUID;

public interface IShadowBlockEntity {
    UUID sodium$getOwner();

    void sodium$setOwner(UUID uuid);

    default boolean sodium$isOwner(UUID uuid) {
        return Objects.equals(uuid, sodium$getOwner());
    }

    default boolean sodium$canOpenBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.hasPermissionLevel(2) || sodium$canOpenBlock(player.getUuid());
    }

    default boolean sodium$canModifyBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.hasPermissionLevel(2) || sodium$canModifyBlock(player.getUuid());
    }

    default boolean sodium$canBreakBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.hasPermissionLevel(2) || sodium$canBreakBlock(player.getUuid());
    }

    // Note: This doesn't apply to doors.
    // Note: This however does apply to beds.
    boolean sodium$canOpenBlock(UUID uuid);

    boolean sodium$canModifyBlock(UUID uuid);

    boolean sodium$canBreakBlock(UUID uuid);

    /**
     * Used to prevent the tile entity from being sent to the client if
     * the block for it isn't visible. This is to prevent cheating out chests
     * and mob spawners even if the blocks themselves aren't present.
     * <p>
     * This can also be applied to non-vanilla tile entities, preventing them
     * from ever being sent.
     *
     * @return true if the tile entity should be sent to the client.
     */
    boolean sodium$isVisible();

    void sodium$setVisible(boolean visibility);
}
