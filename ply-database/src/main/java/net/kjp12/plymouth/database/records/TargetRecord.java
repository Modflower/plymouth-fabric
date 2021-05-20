package net.kjp12.plymouth.database.records;// Created 2021-07-05T12:28:12

import net.kjp12.plymouth.common.UUIDHelper;
import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.Target;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.UUID;

/**
 * A minimal implementation of {@link Target} for use with passing along to record constructors and anything that accepts Target.
 * <p>
 * Holds the world, position, name, user ID and entity ID.
 *
 * @author KJP12
 * @since ${version}
 **/
public final class TargetRecord implements Target {
    public final ServerWorld world;
    public final BlockPos pos;
    public final Vec3d dpos;
    // This isn't counted in the hash, it is effectively equal regardless of if the name matches.
    public final String name;
    public final UUID userId, entityId;

    public TargetRecord(ServerWorld world, BlockPos pos, Vec3d dpos, String name, UUID userId, UUID entityId) {
        this.world = world;
        this.pos = pos == null ? dpos == null ? null : new BlockPos(dpos) : pos.toImmutable();
        this.dpos = dpos == null ? pos == null ? null : Vec3d.ofCenter(pos) : dpos;
        this.name = name;
        this.userId = userId;
        this.entityId = entityId;
    }

    public TargetRecord(ServerWorld world, BlockPos pos) {
        this(world, pos, null, null, null, null);
    }

    public TargetRecord(Entity entity) {
        this((ServerWorld) entity.world, entity.getBlockPos(), entity.getPos(), DatabaseHelper.getName(entity), UUIDHelper.getUUID(entity), entity instanceof PlayerEntity ? null : entity.getUuid());
    }

    public static TargetRecord ofEntityNoPosition(Entity entity) {
        return new TargetRecord(null, null, null, DatabaseHelper.getName(entity), UUIDHelper.getUUID(entity), entity instanceof PlayerEntity ? null : entity.getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetRecord)) return false;
        TargetRecord that = (TargetRecord) o;
        return Objects.equals(world, that.world) && Objects.equals(pos, that.pos) && Objects.equals(userId, that.userId) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        int i = Objects.hashCode(world);
        i = 31 * i + Objects.hashCode(pos);
        i = 31 * i + Objects.hashCode(dpos);
        i = 31 * i + Objects.hashCode(userId);
        return 31 * i + Objects.hashCode(entityId);
    }

    @Override
    public String toString() {
        return "TargetRecord{" +
                "world=" + world +
                ", pos=" + pos +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                ", entityId=" + entityId +
                '}';
    }

    @Override
    public TargetRecord plymouth$toRecord() {
        return this;
    }

    @Override
    public ServerWorld ply$world() {
        return world;
    }

    @Override
    public BlockPos ply$pos3i() {
        return pos;
    }

    @Override
    public Vec3d ply$pos3d() {
        return dpos;
    }

    @Override
    public String ply$name() {
        return name;
    }

    @Override
    public UUID ply$userId() {
        return userId;
    }

    @Override
    public UUID ply$entityId() {
        return entityId;
    }
}
