package net.kjp12.plymouth.database.records;// Created 2021-07-05T11:01:13

import net.kjp12.plymouth.database.BlockAction;
import net.kjp12.plymouth.database.TextUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.Instant;
import java.util.UUID;

import static net.kjp12.plymouth.database.DatabaseHelper.tookText;

/**
 * A record of inventory changes.
 * <p>
 * Holds the mutator and target of inventories, including the world, the pos, and whom,
 * the time the action was done, the item that was placed or taken and how much.
 *
 * @author KJP12
 * @since ${version}
 **/
public final class InventoryRecord implements PlymouthRecord {
    public final World causeWorld, targetWorld;
    public final BlockPos causePos, targetPos;
    public final Instant time;
    public final boolean isUndone;
    public final Item item;
    public final NbtCompound nbt;
    public final String causeName, targetName;
    public final UUID causeUserId, causeEntityId, targetUserId, targetEntityId;
    /**
     * How many items of {@link #item} and {@link #nbt} got transferred in or out of the target inventory.
     * <p>
     * Negative numbers indicate how much was removed.
     */
    public int delta;
    public final transient ItemStack stack;
    private final int flags;

    public InventoryRecord(World causeWorld, BlockPos causePos, String causeName, UUID causeUserId, UUID causeEntityId,
                           World targetWorld, BlockPos targetPos, String targetName, UUID targetUserId, UUID targetEntityId,
                           Instant time, boolean isUndone, Item item, NbtCompound nbt, int delta, ItemStack stack, int flags) {
        this.causeWorld = causeWorld;
        this.causePos = causePos;
        if (causeName == null && causeUserId != null) throw new Error("cause wtf?");
        this.causeName = causeName;
        this.causeUserId = causeUserId;
        this.causeEntityId = causeEntityId;
        this.targetWorld = targetWorld;
        this.targetPos = targetPos;
        if (targetName == null && targetUserId != null) throw new Error("target wtf?");
        this.targetName = targetName;
        this.targetUserId = targetUserId;
        this.targetEntityId = targetEntityId;
        this.time = time;
        this.isUndone = isUndone;
        this.item = item;
        this.nbt = nbt;
        this.delta = delta;
        this.stack = stack;
        this.flags = flags;
    }

    /**
     * Constructs an InventoryRecord based off cause, target, the stack and the amount moved.
     *
     * @param cause  The mutator of the inventory.
     * @param target The inventory that was mutated.
     * @param stack  The reference stack of the item that was added or removed.
     * @param delta  Amount of items added or removed. Negative numbers means removed.
     */
    public InventoryRecord(TargetRecord cause, TargetRecord target, ItemStack stack, int delta) {
        this(cause.world, cause.pos, cause.name, cause.userId, cause.entityId,
                target.world, target.pos, target.name, target.userId, target.entityId,
                Instant.now(), false, stack.getItem(), stack.getTag(), delta, stack,
                // TODO: Flags??
                (cause.pos != null ? 2 : 0) | (target.pos != null ? 1 : 0));
    }

    @Override
    public String toString() {
        return "InventoryRecord{" +
                "causeWorld=" + causeWorld +
                ", targetWorld=" + targetWorld +
                ", causePos=" + causePos +
                ", targetPos=" + targetPos +
                ", time=" + time +
                ", isUndone=" + isUndone +
                ", item=" + item +
                ", nbt=" + nbt +
                ", causeName='" + causeName + '\'' +
                ", targetName='" + targetName + '\'' +
                ", causeUserId=" + causeUserId +
                ", causeEntityId=" + causeEntityId +
                ", targetUserId=" + targetUserId +
                ", targetEntityId=" + targetEntityId +
                ", delta=" + delta +
                ", stack=" + stack +
                '}';
    }

    @Override
    public @NotNull Text toText() {
        var text = new TranslatableText("plymouth.tracker.record.inventory",
                TextUtils.timeToText(time),
                TextUtils.playerToText(causeWorld, causePos, causeName, causeUserId, causeEntityId),
                delta < 0 ? tookText : BlockAction.PLACE.niceName, Math.abs(delta), TextUtils.itemToText(stack),
                TextUtils.playerToText(targetWorld, targetPos, targetName, targetUserId, targetEntityId));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    @Override
    public @NotNull Text toTextNoPosition() {
        var text = new TranslatableText("plymouth.tracker.record.inventory.nopos",
                TextUtils.timeToText(time),
                TextUtils.playerToText(causeWorld, causePos, causeName, causeUserId, causeEntityId),
                delta < 0 ? tookText : BlockAction.PLACE.niceName, Math.abs(delta), TextUtils.itemToText(stack));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    /**
     * @return Flags indicating if the cause and target are blocks.
     */
    @Override
    public int flags() {
        return flags;
    }

    @Override
    public RecordType getType() {
        return RecordType.INVENTORY;
    }

    /**
     * Creates an input stream based off of the NBT passed for the item.
     *
     * @return InputStream if NBT exists, else null.
     */
    @Nullable
    public InputStream mkNbtStream() {
        if (nbt == null) return null;
        try (var baos = new ByteArrayOutputStream(); var dos = new DataOutputStream(baos)) {
            NbtIo.write(nbt, dos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }
}
