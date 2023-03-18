package net.kjp12.plymouth.database.records;// Created 2021-13-06T05:13:42

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class BlockLookupRecord extends LookupRecord<BlockRecord> {
    public final ServerWorld targetWorld;
    public final BlockPos minTPos, maxTPos;
    public final BlockState beforeState, afterState;

    public BlockLookupRecord(ServerWorld world, BlockPos minPosition, BlockPos maxPosition, UUID causeUuid, Instant minTime, Instant maxTime,
                             ServerWorld targetWorld, BlockPos minTPos, BlockPos maxTPos, BlockState beforeState, BlockState afterState, int page, int flags) {
        super(world, minPosition, maxPosition, causeUuid, minTime, maxTime, page, flags);
        switch (flags >>> 6 & 3) {
            case 0 -> {
                this.targetWorld = null;
                this.minTPos = null;
                this.maxTPos = null;
            }
            case 1 -> {
                this.targetWorld = Objects.requireNonNull(targetWorld, "targetWorld");
                this.minTPos = minTPos.toImmutable();
                this.maxTPos = null;
            }
            case 2 -> {
                this.targetWorld = Objects.requireNonNull(targetWorld, "targetWorld");
                int ax = minTPos.getX(), ay = minTPos.getY(), az = minTPos.getZ(),
                        bx = maxTPos.getX(), by = maxTPos.getY(), bz = maxTPos.getZ(),
                        ix = Math.min(ax, bx), iy = Math.min(ay, by), iz = Math.min(az, bz);
                if (ax == ix && ay == iy && az == iz) {
                    this.minTPos = minTPos.toImmutable();
                    this.maxTPos = maxTPos.toImmutable();
                } else {
                    this.minTPos = new BlockPos(ix, iy, iz);
                    this.maxTPos = new BlockPos(Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
                }
            }
            default -> throw new IllegalStateException("Illegal state 3 on AT & AREA for given flags " + flags);
        }
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public BlockLookupRecord(ServerWorld world, BlockPos pos, int page) {
        this(null, null, null, null, null, null, world, pos, null, null, null, page, FLAG_T_AT);
    }

    @Override
    public RecordType getType() {
        return RecordType.LOOKUP_BLOCK;
    }

    @Override
    public Class<BlockRecord> getOutput() {
        return BlockRecord.class;
    }

    public int minTX() {
        return minTPos.getX();
    }

    public int minTY() {
        return minTPos.getY();
    }

    public int minTZ() {
        return minTPos.getZ();
    }

    public int maxTX() {
        return maxTPos.getX();
    }

    public int maxTY() {
        return maxTPos.getY();
    }

    public int maxTZ() {
        return maxTPos.getZ();
    }
}
