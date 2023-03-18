package net.kjp12.plymouth.database;// Created 2021-10-05T22:28:41

import net.kjp12.plymouth.database.records.TargetRecord;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Target implementation that covers the minimum requirements to be registered within the database.
 *
 * @author Ampflower
 * @since ${version}
 **/
public interface Target {
    default TargetRecord plymouth$toRecord() {
        return new TargetRecord(ply$world(), ply$pos3i(), ply$pos3d(), ply$name(), ply$userId(), ply$entityId());
    }

    default boolean ply$isBlock() {
        return false;
    }

    World ply$world();

    default World ply$blockWorld() {
        return ply$isBlock() ? ply$world() : null;
    }

    BlockPos ply$pos3i();

    default BlockPos ply$blockPos3i() {
        return ply$isBlock() ? ply$pos3i() : null;
    }

    default Vec3d ply$pos3d() {
        return Vec3d.ofBottomCenter(ply$pos3i());
    }

    String ply$name();

    UUID ply$userId();

    UUID ply$entityId();

    /**
     * An equality method to determine if the targets describes the same entity or block.
     *
     * @param other The target to test against.
     * @return true if other is this or if other is the same user, entity and, if a block, world and position.
     */
    default boolean ply$targetMatches(@Nullable Target other) {
        return this == other || (other != null &&
                Objects.equals(ply$userId(), other.ply$userId()) &&
                Objects.equals(ply$entityId(), other.ply$entityId()) &&
                ply$isBlock() == other.ply$isBlock() &&
                (!ply$isBlock() || Objects.equals(ply$pos3i(), other.ply$pos3i()) && Objects.equals(ply$world(), other.ply$world())));
    }
}
