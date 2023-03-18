package net.kjp12.plymouth.database.records;// Created 2021-01-05T22:28:41

import net.kjp12.plymouth.common.UUIDHelper;
import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.database.TextUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

import static net.kjp12.plymouth.database.TextUtils.atBlock;

/**
 * A record of an entity death.
 * <p>
 * Holds the killer and the target, including the world, the position and whom, and the time the death occurred.
 *
 * @author Ampflower
 * @since ${version}
 **/
public final class DeathRecord implements PlymouthRecord {
    public final World causeWorld, targetWorld;
    public final BlockPos causePos;
    public final Vec3d targetPos;
    public final Instant time;
    public final boolean isUndone;
    public final String causeName, targetName;
    public final UUID causeUserId, causeEntityId, targetUserId, targetEntityId;

    public DeathRecord(Instant time, boolean isUndone,
                       World causeWorld, BlockPos causePos, String causeName, UUID causeUserId, UUID causeEntityId,
                       World targetWorld, Vec3d targetPos, String targetName, UUID targetUserId, UUID targetEntityId) {
        this.causeWorld = causeWorld;
        this.causePos = causePos;
        this.targetWorld = targetWorld;
        this.targetPos = targetPos;
        this.time = time;
        this.isUndone = isUndone;
        this.causeName = causeName;
        this.causeUserId = causeUserId;
        this.causeEntityId = causeEntityId;
        this.targetName = targetName;
        this.targetUserId = targetUserId;
        this.targetEntityId = targetEntityId;
    }

    /**
     * Helper constructor for defaulting time to now, isUndone to false, and assuming that the cause is always an entity.
     */
    public DeathRecord(World causeWorld, BlockPos causePos, String causeName, UUID causeUserId, UUID causeEntityId,
                       World targetWorld, Vec3d targetPos, String targetName, UUID targetUserId, UUID targetEntityId) {
        this(Instant.now(), false,
                causeWorld, causePos, causeName, causeUserId, causeEntityId,
                targetWorld, targetPos, targetName, targetUserId, targetEntityId);
    }

    /**
     * Creates a DeathRecord based off of the causing and target entities.
     *
     * @param cause  The killing entity.
     * @param target The entity that was killed.
     */
    public DeathRecord(Target cause, Target target) {
        this(cause.ply$blockWorld(), cause.ply$blockPos3i(), cause.ply$name(), cause.ply$userId(), cause.ply$entityId(),
                target.ply$world(), target.ply$pos3d(), target.ply$name(), target.ply$userId(), target.ply$entityId());
    }

    /**
     * Creates a DeathRecord based off of a DamageSource and Entity.
     *
     * @param cause  The killing damage source.
     * @param target The entity that was killed.
     * @return The death record derived from the cause and target.
     */
    public static DeathRecord fromDamageSource(DamageSource cause, Entity target) {
        var causeUser = UUIDHelper.getEntity(cause);
        return new DeathRecord(null, null, DatabaseHelper.getName(cause), UUIDHelper.getUUID(cause), causeUser == null || causeUser instanceof PlayerEntity ? null : causeUser.getUuid(),
                target.world, target.getPos(), DatabaseHelper.getName(target), UUIDHelper.getUUID(target), target instanceof PlayerEntity ? null : target.getUuid());
    }

    @Override
    public @NotNull Text toText() {
        var text = Text.translatable("plymouth.tracker.record.death", TextUtils.timeToText(time), TextUtils.playerToText(causeName, causeUserId, causeEntityId), TextUtils.playerToText(targetName, targetUserId, targetEntityId),
                Text.translatable("chat.coordinates", (long) targetPos.x, (long) targetPos.y, (long) targetPos.z)
                        .setStyle(atBlock.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("X: ")
                                        .append(Text.literal(Double.toString(targetPos.x)).formatted(Formatting.RED)).append("\nY: ")
                                        .append(Text.literal(Double.toString(targetPos.y)).formatted(Formatting.GREEN)).append("\nZ: ")
                                        .append(Text.literal(Double.toString(targetPos.z)).formatted(Formatting.BLUE))
                        ))));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    @Override
    public @NotNull Text toTextNoPosition() {
        var text = Text.translatable("plymouth.tracker.record.death.nopos", TextUtils.timeToText(time), TextUtils.playerToText(causeName, causeUserId, causeEntityId), TextUtils.playerToText(targetName, targetUserId, targetEntityId));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    @Override
    public RecordType getType() {
        return RecordType.DEATH;
    }

    @Override
    public String toString() {
        return "DeathRecord{causeWorld=" + causeWorld + ",causePos=" + causePos + ",targetWorld=" + targetWorld + ",targetPos=" + targetPos + ",time=" + time + ",isUndone=" + isUndone + ",causeName=" + causeName + ",causeUserId=" + causeUserId + ",causeEntityId=" + causeEntityId + ",targetName=" + targetName + ",targetUserId=" + targetUserId + ",targetEntityId=" + targetEntityId + '}';
    }
}