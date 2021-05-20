package gay.ampflower.plymouth.database;

import gay.ampflower.plymouth.database.records.TargetRecord;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

    ServerWorld ply$world();

    default ServerWorld ply$blockWorld() {
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
}
