package gay.ampflower.sodium.helpers;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;
import java.util.UUID;

public interface IProtectBlock {
    UUID sodium$getOwner();

    void sodium$setOwner(UUID uuid);

    default boolean sodium$isOwner(UUID uuid) {
        return Objects.equals(uuid, sodium$getOwner());
    }

    default boolean sodium$canOpenBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.isCreativeLevelTwoOp() || sodium$canOpenBlock(player.getUuid());
    }

    default boolean sodium$canModifyBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.isCreativeLevelTwoOp() || sodium$canModifyBlock(player.getUuid());
    }

    default boolean sodium$canBreakBlock(PlayerEntity player) {
        return sodium$getOwner() == null || player.isCreativeLevelTwoOp() || sodium$canBreakBlock(player.getUuid());
    }

    // Note: This doesn't apply to doors.
    // Note: This however does apply to beds.
    boolean sodium$canOpenBlock(UUID uuid);

    boolean sodium$canModifyBlock(UUID uuid);

    boolean sodium$canBreakBlock(UUID uuid);
}
