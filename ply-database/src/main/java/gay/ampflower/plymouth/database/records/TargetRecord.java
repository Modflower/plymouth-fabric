package gay.ampflower.plymouth.database.records;

import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.UUID;

/**
 * A minimal implementation of {@link Target} for use with passing along to record constructors and anything that accepts Target.
 * <p>
 * Holds the world, position, name, user ID and entity ID.
 *
 * @author Ampflower
 * @since ${version}
 **/
public final class TargetRecord implements Target {
    public final World world;
    public final BlockPos pos;
    public final Vec3d dpos;
    // This isn't counted in the hash, it is effectively equal regardless of if the name matches.
    public final String name;
    public final UUID userId, entityId;

    public TargetRecord(World world, BlockPos pos, Vec3d dpos, String name, UUID userId, UUID entityId) {
        this.world = world;
        this.pos = pos == null ? dpos == null ? null : new BlockPos(dpos) : pos.toImmutable();
        this.dpos = dpos == null ? pos == null ? null : Vec3d.ofCenter(pos) : dpos;
        if (name == null && userId != null) throw new Error("wtf?");
        this.name = name;
        this.userId = userId;
        this.entityId = entityId;
    }

    public TargetRecord(World world, BlockPos pos) {
        this(world, pos, null, null, null, null);
    }

    public TargetRecord(Entity entity) {
        this(entity.world, entity.getBlockPos(), entity.getPos(), DatabaseHelper.getName(entity), UUIDHelper.getUUID(entity), entity instanceof PlayerEntity ? null : entity.getUuid());
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
    public World ply$world() {
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
