package gay.ampflower.plymouth.database.records;

import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class InventoryLookupRecord extends LookupRecord<InventoryRecord> {
    public final ServerWorld targetWorld;
    public final BlockPos minTPos, maxTPos;
    public final Item item;
    public final UUID targetUserId, targetEntityId;

    public InventoryLookupRecord(ServerWorld world, BlockPos minPosition, BlockPos maxPosition, UUID causeUuid, Instant minTime, Instant maxTime,
                                 ServerWorld targetWorld, BlockPos minTPos, BlockPos maxTPos, Item item, UUID targetUserId, UUID targetEntityId, int page, int flags) {
        super(world, minPosition, maxPosition, causeUuid, minTime, maxTime, page, flags);
        this.targetWorld = targetWorld;
        this.minTPos = minTPos;
        this.maxTPos = maxTPos;
        this.item = item;
        this.targetUserId = targetUserId;
        this.targetEntityId = targetEntityId;
    }

    public InventoryLookupRecord(ServerWorld world, BlockPos pos, int page) {
        this(null, null, null, null, null, null, world, pos, null, null, null, null, page, FLAG_T_AT);
    }

    public String item() {
        return Registry.ITEM.getId(item).toString();
    }

    @Override
    public RecordType getType() {
        return RecordType.LOOKUP_INVENTORY;
    }

    @Override
    public Class<InventoryRecord> getOutput() {
        return InventoryRecord.class;
    }
}
