package net.kjp12.plymouth.database.records;// Created 2021-02-05T23:19:10

import net.kjp12.hachimitsu.database.api.DatabaseRecord;
import net.kjp12.plymouth.database.DatabaseHelper;
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
public abstract class LookupRecord<T extends PlymouthRecord> implements DatabaseRecord<List<T>>, PlymouthRecord, CompletableRecord<List<T>> {
    /**
     * Indicates that lookup is by user.
     */
    public static final int FLAG_C_UID = 0x01, FLAG_T_UID = 0x10, FLAG_C_EID = 0x100, FLAG_T_EID = 0x1000;
    /**
     * Indicates that lookup is by time.
     */
    public static final int FLAG_MIN_TIME = 0x02, FLAG_MAX_TIME = 0x20;
    /**
     * Indicates that lookup is by location.
     * <p>
     * Mutually exclusive with {@link #FLAG_C_AREA}.
     */
    public static final int FLAG_C_AT = 0x04, FLAG_T_AT = 0x40;
    /**
     * Indicates that lookup is by area.
     * <p>
     * Mutually exclusive with {@link #FLAG_C_AT}.
     */
    public static final int FLAG_C_AREA = 0x08, FLAG_T_AREA = 0x80;

    public static final int FLAG_ITEM = 0x200;

    private final CompletableFuture<List<T>> future;
    public final ServerWorld causeWorld;
    public final BlockPos minPos, maxPos;
    public final UUID causeUserId;
    public final Instant minTime, maxTime;
    public final int page, limit;
    private final int flags;

    public LookupRecord(ServerWorld causeWorld, BlockPos minPos, BlockPos maxPos, UUID causeUserId, Instant minTime, Instant maxTime, int page, int flags) {
        this.future = new CompletableFuture<>();
        switch (flags >>> 2 & 3) {
            case 0 -> {
                this.causeWorld = null;
                this.minPos = null;
                this.maxPos = null;
            }
            case 1 -> {
                this.causeWorld = Objects.requireNonNull(causeWorld, "causeWorld");
                this.minPos = minPos.toImmutable();
                this.maxPos = null;
            }
            case 2 -> {
                this.causeWorld = Objects.requireNonNull(causeWorld, "causeWorld");
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
            }
            default -> throw new IllegalStateException("Illegal state 3 on AT & AREA for given flags " + flags);
        }
        this.causeUserId = causeUserId;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.page = page;
        this.limit = DatabaseHelper.PAGE_SIZE;
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
        return switch (flags & 0b1111) {
            case 0b0000 -> new TranslatableText("plymouth.tracker.record.lookup", page);
            case 0b0001 -> new TranslatableText("plymouth.tracker.record.lookup.by", lookupPlayerToText(null, causeUserId), page);
            case 0b0010 -> new TranslatableText("plymouth.tracker.record.lookup.time", minTime, maxTime, page);
            case 0b0011 -> new TranslatableText("plymouth.tracker.record.lookup.time.by", minTime, maxTime, lookupPlayerToText(null, causeUserId), page);
            case 0b0100 -> new TranslatableText("plymouth.tracker.record.lookup.at", positionToText(minPos), page);
            case 0b0101 -> new TranslatableText("plymouth.tracker.record.lookup.at.by", positionToText(minPos), lookupPlayerToText(null, causeUserId), page);
            case 0b0110 -> new TranslatableText("plymouth.tracker.record.lookup.at.time", positionToText(minPos), minTime, maxTime, page);
            case 0b0111 -> new TranslatableText("plymouth.tracker.record.lookup.at.time.by", positionToText(minPos), minTime, maxTime, lookupPlayerToText(null, causeUserId), page);
            case 0b1000 -> new TranslatableText("plymouth.tracker.record.lookup.area", positionToText(minPos), positionToText(maxPos), page);
            case 0b1001 -> new TranslatableText("plymouth.tracker.record.lookup.area.by", positionToText(minPos), positionToText(maxPos), lookupPlayerToText(null, causeUserId), page);
            case 0b1010 -> new TranslatableText("plymouth.tracker.record.lookup.area.time", positionToText(minPos), positionToText(maxPos), minTime, maxTime, page);
            case 0b1011 -> new TranslatableText("plymouth.tracker.record.lookup.area.time.by", positionToText(minPos), positionToText(maxPos), minTime, maxTime, lookupPlayerToText(null, causeUserId), page);
            default -> new TranslatableText("plymouth.tracker.record.lookup.invalid");
        };
    }

    /**
     * Flags for what this lookup is for.
     *
     * <h3>Bits 3-0: Flags</h3>
     * <ul>
     *     <li><code>0x0</code> ALL
     *     <li><code>0x1</code> {@link #FLAG_C_UID BY}</li>
     *     <li><code>0x2</code> {@link #FLAG_MIN_TIME TIME}</li>
     *     <li><code>0x4</code> {@link #FLAG_C_AT AT}</li>
     *     <li><code>0x8</code> {@link #FLAG_C_AREA AREA}</li>
     * </ul>
     */
    @Override
    public int flags() {
        return flags;
    }

    public int limit() {
        return this.limit;
    }

    public int offset() {
        return page * limit;
    }

    public abstract Class<T> getOutput();
}
