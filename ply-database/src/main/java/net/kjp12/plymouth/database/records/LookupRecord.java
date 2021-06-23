package net.kjp12.plymouth.database.records;// Created 2021-02-05T23:19:10

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static net.kjp12.plymouth.database.TextUtils.lookupPlayerToText;
import static net.kjp12.plymouth.database.TextUtils.positionToText;

/**
 * A record for looking up records in a database.
 * <p>
 * Capable of searching by all, whom, time and location for a certain record type.
 * <p>
 * Note that by default, the record will lookup all block records on page 1.
 *
 * @author KJP12
 * @since ${version}
 **/
public abstract class LookupRecord<T extends PlymouthRecord> implements PlymouthRecord, CompletableRecord<List<T>> {
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
     * Indicates that lookup is for deaths.
     */
    @Deprecated(forRemoval = true)
    public static final int MOD_DEATH = 0x10;

    private final CompletableFuture<List<T>> future;
    public final ServerWorld causeWorld;
    public final BlockPos minPos, maxPos;
    public final UUID causeUuid;
    public final Instant minTime, maxTime;
    public final int page;
    private final int flags;

    public LookupRecord(ServerWorld causeWorld, BlockPos minPos, BlockPos maxPos, UUID causeUuid, Instant minTime, Instant maxTime, int page, int flags) {
        this.future = new CompletableFuture<>();
        this.causeWorld = causeWorld;
        switch (flags >>> 2 & 3) {
            case 0:
                this.minPos = null;
                this.maxPos = null;
                break;
            case 1:
                this.minPos = minPos.toImmutable();
                this.maxPos = null;
                break;
            case 2:
                int ax = minPos.getX(), ay = minPos.getY(), az = minPos.getZ(),
                        bx = maxPos.getX(), by = maxPos.getY(), bz = maxPos.getZ(),
                        ix = Math.min(ax, bx), iy = Math.min(ay, by), iz = Math.min(az, bz);
                if (ax == ix && ay == iy && az == iz) {
                    this.minPos = minPos.toImmutable();
                    this.maxPos = maxPos.toImmutable();
                } else {
                    this.minPos = new BlockPos(ix, iy, iz);
                    this.maxPos = new BlockPos(Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
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

    @Override
    public void complete(List<T> object) {
        this.future.complete(object);
    }

    @Override
    public void fail(Throwable throwable) {
        this.future.completeExceptionally(throwable);
    }

    @Override
    public CompletionStage<List<T>> getFuture() {
        return future.minimalCompletionStage();
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
                return new TranslatableText("plymouth.tracker.record.lookup.at", positionToText(minPos), page);
            case 0b0101:
                return new TranslatableText("plymouth.tracker.record.lookup.at.by", positionToText(minPos), lookupPlayerToText(null, causeUuid), page);
            case 0b0110:
                return new TranslatableText("plymouth.tracker.record.lookup.at.time", positionToText(minPos), minTime, maxTime, page);
            case 0b0111:
                return new TranslatableText("plymouth.tracker.record.lookup.at.time.by", positionToText(minPos), minTime, maxTime, lookupPlayerToText(null, causeUuid), page);
            case 0b1000:
                return new TranslatableText("plymouth.tracker.record.lookup.area", positionToText(minPos), positionToText(maxPos), page);
            case 0b1001:
                return new TranslatableText("plymouth.tracker.record.lookup.area.by", positionToText(minPos), positionToText(maxPos), lookupPlayerToText(null, causeUuid), page);
            case 0b1010:
                return new TranslatableText("plymouth.tracker.record.lookup.area.time", positionToText(minPos), positionToText(maxPos), minTime, maxTime, page);
            case 0b1011:
                return new TranslatableText("plymouth.tracker.record.lookup.area.time.by", positionToText(minPos), positionToText(maxPos), minTime, maxTime, lookupPlayerToText(null, causeUuid), page);
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

    public abstract Class<T> getOutput();
}
