package net.kjp12.plymouth.database.records;// Created 2021-15-06T11:07:12

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * @author KJP12
 * @since ${version}
 **/
public final class DeathLookupRecord extends LookupRecord<DeathRecord> {
    public final ServerWorld targetWorld;
    public final Vec3d minTPos, maxTPos;
    public final UUID targetUserId, targetEntityId;

    public DeathLookupRecord(ServerWorld world, BlockPos minPos, BlockPos maxPos, UUID causeUuid, Instant minTime, Instant maxTime,
                             ServerWorld targetWorld, Vec3d minTPos, Vec3d maxTPos, UUID targetUserId, UUID targetEntityId, int page, int flags) {
        super(world, minPos, maxPos, causeUuid, minTime, maxTime, page, flags);
        switch (flags >>> 6 & 3) {
            case 0 -> {
                this.targetWorld = null;
                this.minTPos = null;
                this.maxTPos = null;
            }
            case 1 -> {
                this.targetWorld = Objects.requireNonNull(targetWorld, "targetWorld");
                this.minTPos = minTPos;
                this.maxTPos = null;
            }
            case 2 -> {
                this.targetWorld = Objects.requireNonNull(targetWorld, "targetWorld");
                double ax = minTPos.getX(), ay = minTPos.getY(), az = minTPos.getZ(),
                        bx = maxTPos.getX(), by = maxTPos.getY(), bz = maxTPos.getZ(),
                        ix = Math.min(ax, bx), iy = Math.min(ay, by), iz = Math.min(az, bz);
                if (ax == ix && ay == iy && az == iz) {
                    this.minTPos = minTPos;
                    this.maxTPos = maxTPos;
                } else {
                    this.minTPos = new Vec3d(ix, iy, iz);
                    this.maxTPos = new Vec3d(Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
                }
            }
            default -> throw new IllegalStateException("Illegal state 3 on AT & AREA for given flags " + flags);
        }
        this.targetUserId = targetUserId;
        this.targetEntityId = targetEntityId;
    }

    @Override
    public Class<DeathRecord> getOutput() {
        return DeathRecord.class;
    }

    @Override
    public RecordType getType() {
        return RecordType.LOOKUP_DEATH;
    }

    public double minTX() {
        return minTPos.getX();
    }

    public double minTY() {
        return minTPos.getY();
    }

    public double minTZ() {
        return minTPos.getZ();
    }

    public double maxTX() {
        return maxTPos.getX();
    }

    public double maxTY() {
        return maxTPos.getY();
    }

    public double maxTZ() {
        return maxTPos.getZ();
    }
}
