package net.kjp12.plymouth.database.records;// Created 2021-07-04T06:56

import net.kjp12.plymouth.database.BlockAction;
import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.database.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A record of world changes.
 * <p>
 * Holds the mutator and target of blocks, including the world, the pos and whom,
 * the time the action was done, the block that was placed or broken, the NBT if it was a block entity,
 * and what action was done on it.
 *
 * @author KJP12
 * @since ${version}
 **/
public final class BlockRecord implements PlymouthRecord {
    public final ServerWorld causeWorld, targetWorld;
    public final BlockPos causePos, targetPos;
    public final Instant time;
    public final boolean isUndone;
    public final BlockAction action;
    public final BlockState block;
    public final NbtCompound nbt;
    public final String userName;
    public final UUID userId, entityId;

    public BlockRecord(Instant time, boolean isUndone,
                       ServerWorld causeWorld, BlockPos causePos, String userName, UUID userId, UUID entityId,
                       ServerWorld targetWorld, BlockPos targetPos, BlockAction action, BlockState block, NbtCompound nbt) {
        this.causeWorld = causeWorld;
        this.causePos = causePos;
        this.targetWorld = targetWorld;
        this.targetPos = targetPos;
        this.time = time;
        this.isUndone = isUndone;
        this.action = action;
        this.block = block;
        this.nbt = nbt;
        this.userName = userName;
        this.userId = userId;
        this.entityId = entityId;
    }

    /**
     * Now helper constructor, setting time to the instance it was called and isUndone to false.
     */
    public BlockRecord(ServerWorld causeWorld, BlockPos causePos, String userName, UUID userId, UUID entityId,
                       ServerWorld targetWorld, BlockPos targetPos, BlockAction action, BlockState state, NbtCompound nbt) {
        this(Instant.now(), false, causeWorld, causePos, userName, userId, entityId, targetWorld, targetPos, action, state, nbt);
    }

    /**
     * Constructs a now-based BlockRecord with NBT.
     */
    public BlockRecord(Target cause, ServerWorld targetWorld, BlockPos targetPos, BlockAction action, BlockState state, NbtCompound nbt) {
        this(null, null, cause.ply$name(), cause.ply$userId(), cause.ply$entityId(), targetWorld, targetPos, action, state, nbt);
    }

    /**
     * Constructs a now-based BlockRecord without NBT.
     */
    public BlockRecord(Target cause, ServerWorld targetWorld, BlockPos targetPos, BlockAction action, BlockState state) {
        this(null, null, cause.ply$name(), cause.ply$userId(), cause.ply$entityId(), targetWorld, targetPos, action, state, null);
    }

    @Override
    public String toString() {
        return "BlockRecord{causePos=" + causePos + ",targetPos=" + targetPos + ",time=" + time + ",undone=" + isUndone + ",action=" + action + ",block=" + block + ",name=" + userName + ",userId=" + userId + ",entityId=" + entityId + '}';
    }

    @Override
    @NotNull
    public Text toText() {
        var text = Text.translatable("plymouth.tracker.record.block", TextUtils.timeToText(time), TextUtils.playerToText(userName, userId, entityId), action.niceName, TextUtils.blockToText(block), TextUtils.positionToText(targetPos).setStyle(TextUtils.atBlock));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    @Override
    @NotNull
    public Text toTextNoPosition() {
        var text = Text.translatable("plymouth.tracker.record.block.nopos", TextUtils.timeToText(time), TextUtils.playerToText(userName, userId, entityId), action.niceName, TextUtils.blockToText(block));
        if (isUndone) text.formatted(Formatting.STRIKETHROUGH);
        return text;
    }

    @Override
    public final RecordType getType() {
        return RecordType.BLOCK;
    }

    /**
     * Creates an input stream based off of the NBT passed for the block.
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
