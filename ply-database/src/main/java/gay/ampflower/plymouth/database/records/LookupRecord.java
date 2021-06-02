package gay.ampflower.plymouth.database.records;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static gay.ampflower.plymouth.database.TextUtils.lookupPlayerToText;
import static gay.ampflower.plymouth.database.TextUtils.positionToText;

/**
 * A record for looking up records in a database.
 * <p>
 * Capable of searching by all, whom, time and location for a certain record type.
 * <p>
 * Note that by default, the record will lookup all block records on page 1.
 *
 * @author Ampflower
 * @since ${version}
 **/
public final class LookupRecord implements PlymouthRecord, CompletableRecord<List<PlymouthRecord>> {
    /**
     * Indicates that nothing has been selected. More for completeness sake.
     */
    public static final int FLAG_NONE = 0x00;
    /**
     * Indicates that lookup is by user.
     */
    public static final int FLAG_BY = 0x01;
    /**
     * Indicates that lookup is by time.
     */
    public static final int FLAG_TIME = 0x02;
    /**
     * Indicates that lookup is by location.
     * <p>
     * Mutually exclusive with {@link #FLAG_AREA}.
     */
    public static final int FLAG_AT = 0x04;
    /**
     * Indicates that lookup is by area.
     * <p>
     * Mutually exclusive with {@link #FLAG_AT}.
     */
    public static final int FLAG_AREA = 0x08;

    /**
     * Indicates that lookup is for blocks.
     */
    public static final int MOD_BLOCK = 0x00;
    /**
     * Indicates that lookup is for deaths.
     */
    public static final int MOD_DEATH = 0x10;
    /**
     * Indicates that lookup is for inventories.
     */
    public static final int MOD_INVEN = 0x20;

    public final CompletableFuture<List<PlymouthRecord>> future;
    public final ServerWorld world;
    public final BlockPos minPosition, maxPosition;
    public final UUID causeUuid;
    public final Instant minTime, maxTime;
    public final int page;
    private final int flags;

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, ServerWorld world, BlockPos minPosition, BlockPos maxPosition, UUID causeUuid, Instant minTime, Instant maxTime, int page, int flags) {
        this.future = future;
        this.world = world;
        switch (flags >>> 2 & 3) {
            case 0:
                this.minPosition = null;
                this.maxPosition = null;
                break;
            case 1:
                this.minPosition = minPosition.toImmutable();
                this.maxPosition = null;
                break;
            case 2:
                int ax = minPosition.getX(), ay = minPosition.getY(), az = minPosition.getZ(),
                        bx = maxPosition.getX(), by = maxPosition.getY(), bz = maxPosition.getZ(),
                        ix = Math.min(ax, bx), iy = Math.min(ay, by), iz = Math.min(az, bz);
                if (ax == ix && ay == iy && az == iz) {
                    this.minPosition = minPosition.toImmutable();
                    this.maxPosition = maxPosition.toImmutable();
                } else {
                    this.minPosition = new BlockPos(ix, iy, iz);
                    this.maxPosition = new BlockPos(Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
                }
                break;
            default:
                throw new IllegalStateException("Illegal state 3 on AT & AREA for given flags " + flags);
        }
        this.causeUuid = causeUuid;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.page = page;
        this.flags = flags;
    }

    // <editor-fold desc="Too many constructors.">
    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos minPosition, BlockPos maxPosition, UUID causeUuid, Instant minTime, Instant maxTime, int page) {
        this(future, world, minPosition, maxPosition, causeUuid, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | FLAG_BY | FLAG_AREA | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos minPosition, BlockPos maxPosition, UUID causeUuid, int page) {
        this(future, world, minPosition, maxPosition, causeUuid, null, null, page, FLAG_BY | FLAG_AREA | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos minPosition, BlockPos maxPosition, Instant minTime, Instant maxTime, int page) {
        this(future, world, minPosition, maxPosition, null, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | FLAG_AREA | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos minPosition, BlockPos maxPosition, int page) {
        this(future, world, minPosition, maxPosition, null, null, null, page, FLAG_AREA | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos pos, UUID causeUuid, Instant minTime, Instant maxTime, int page) {
        this(future, world, pos, null, causeUuid, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | FLAG_BY | FLAG_AT | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos pos, UUID causeUuid, int page) {
        this(future, world, pos, null, causeUuid, null, null, page, FLAG_BY | FLAG_AT | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos pos, Instant minTime, Instant maxTime, int page) {
        this(future, world, pos, null, null, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | FLAG_AT | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, BlockPos pos, int page) {
        this(future, world, pos, null, null, null, null, page, FLAG_AT | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, UUID causeUuid, Instant minTime, Instant maxTime, int page) {
        this(future, world, null, null, causeUuid, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | FLAG_BY | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, UUID causeUuid, int page) {
        this(future, world, null, null, causeUuid, null, null, page, FLAG_BY | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, Instant minTime, Instant maxTime, int page) {
        this(future, world, null, null, null, Objects.requireNonNullElse(minTime, Instant.MIN), Objects.requireNonNullElse(maxTime, Instant.MAX), page, FLAG_TIME | type.bits);
    }

    public LookupRecord(@NotNull CompletableFuture<List<PlymouthRecord>> future, RecordType type, ServerWorld world, int page) {
        this(future, world, null, null, null, null, null, page, FLAG_NONE | type.bits);
    }
    // </editor-fold>

    @Override
    public void complete(List<PlymouthRecord> object) {
        this.future.complete(object);
    }

    @Override
    public void fail(Throwable throwable) {
        this.future.completeExceptionally(throwable);
    }

    @Override
    public @NotNull Text toText() {
        switch (flags & 0b1111) {
            case 0b0000:
                return new TranslatableText("plymouth.tracker.record.lookup", page);
            case 0b0001:
                return new TranslatableText("plymouth.tracker.record.lookup.by", lookupPlayerToText(null, causeUuid), page);
            case 0b0010:
                return new TranslatableText("plymouth.tracker.record.lookup.time", minTime, maxTime, page);
            case 0b0011:
                return new TranslatableText("plymouth.tracker.record.lookup.time.by", minTime, maxTime, lookupPlayerToText(null, causeUuid), page);
            case 0b0100:
                return new TranslatableText("plymouth.tracker.record.lookup.at", positionToText(minPosition), page);
            case 0b0101:
                return new TranslatableText("plymouth.tracker.record.lookup.at.by", positionToText(minPosition), lookupPlayerToText(null, causeUuid), page);
            case 0b0110:
                return new TranslatableText("plymouth.tracker.record.lookup.at.time", positionToText(minPosition), minTime, maxTime, page);
            case 0b0111:
                return new TranslatableText("plymouth.tracker.record.lookup.at.time.by", positionToText(minPosition), minTime, maxTime, lookupPlayerToText(null, causeUuid), page);
            case 0b1000:
                return new TranslatableText("plymouth.tracker.record.lookup.area", positionToText(minPosition), positionToText(maxPosition), page);
            case 0b1001:
                return new TranslatableText("plymouth.tracker.record.lookup.area.by", positionToText(minPosition), positionToText(maxPosition), lookupPlayerToText(null, causeUuid), page);
            case 0b1010:
                return new TranslatableText("plymouth.tracker.record.lookup.area.time", positionToText(minPosition), positionToText(maxPosition), minTime, maxTime, page);
            case 0b1011:
                return new TranslatableText("plymouth.tracker.record.lookup.area.time.by", positionToText(minPosition), positionToText(maxPosition), minTime, maxTime, lookupPlayerToText(null, causeUuid), page);
            default:
                return new TranslatableText("plymouth.tracker.record.lookup.invalid");
        }
    }

    /**
     * Flags for what this lookup is for.
     *
     * <h3>Bits 3-0: Flags</h3>
     * <ul>
     *     <li><code>0x0</code> {@link #FLAG_NONE ALL}</li>
     *     <li><code>0x1</code> {@link #FLAG_BY BY}</li>
     *     <li><code>0x2</code> {@link #FLAG_TIME TIME}</li>
     *     <li><code>0x4</code> {@link #FLAG_AT AT}</li>
     *     <li><code>0x8</code> {@link #FLAG_AREA AREA}</li>
     * </ul>
     * <h3>Bits 5-4: Type</h3>
     * <ul>
     *     <li><code>0</code> {@link #MOD_BLOCK Block}</li>
     *     <li><code>1</code> {@link #MOD_DEATH Death}</li>
     *     <li><code>2</code> {@link #MOD_INVEN Inventory}</li>
     *     <li><code>3</code> Invalid.</li>
     * </ul>
     */
    @Override
    public int flags() {
        return flags;
    }

    @Override
    public RecordType getType() {
        return RecordType.LOOKUP;
    }

    @Override
    public String toString() {
        return "LookupRecord{future=" + future + ",world=" + world + ",minPosition=" + minPosition + ",maxPosition=" + maxPosition + ",causeUuid=" + causeUuid + ",minTime=" + minTime + ",maxTime=" + maxTime + ",flags=" + flags + '}';
    }
}
