package gay.ampflower.plymouth.database.records;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class DeathLookupRecord extends LookupRecord<DeathRecord> {
    public DeathLookupRecord(ServerWorld world, BlockPos minPos, BlockPos maxPos, UUID causeUuid, Instant minTime, Instant maxTime, int page, int flags) {
        super(world, minPos, maxPos, causeUuid, minTime, maxTime, page, flags);
    }

    @Override
    public Class<DeathRecord> getOutput() {
        return DeathRecord.class;
    }
}
