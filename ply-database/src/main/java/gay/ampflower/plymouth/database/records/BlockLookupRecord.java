package gay.ampflower.plymouth.database.records;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.time.Instant;
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
        this.targetWorld = targetWorld;
        this.minTPos = minTPos;
        this.maxTPos = maxTPos;
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public BlockLookupRecord(ServerWorld world, BlockPos pos, int page) {
        this(world, pos, null, null, null, null, null, null, null, null, null, page, 0x00 | FLAG_AT);
    }

    @Override
    public RecordType getType() {
        return RecordType.LOOKUP_BLOCK;
    }

    @Override
    public Class<BlockRecord> getOutput() {
        return BlockRecord.class;
    }
}
