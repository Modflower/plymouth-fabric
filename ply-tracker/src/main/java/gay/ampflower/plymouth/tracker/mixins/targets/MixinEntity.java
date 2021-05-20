package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.database.records.TargetRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(Entity.class)
public class MixinEntity implements Target {
    @Shadow
    protected UUID uuid;
    @Shadow
    public World world;
    @Shadow
    private BlockPos blockPos;
    @Shadow
    private Vec3d pos;
    @Unique
    private TargetRecord record;

    @Override
    public TargetRecord plymouth$toRecord() {
        return record == null ? record = TargetRecord.ofEntityNoPosition((Entity) (Object) this) : record;
    }

    @Override
    public ServerWorld ply$world() {
        return (ServerWorld) world;
    }

    @Override
    public BlockPos ply$pos3i() {
        return blockPos;
    }

    @Override
    public Vec3d ply$pos3d() {
        return pos;
    }

    @Override
    public String ply$name() {
        return DatabaseHelper.getName((Entity) (Object) this);
    }

    @Override
    public UUID ply$userId() {
        return UUIDHelper.getUUID((Entity) (Object) this);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UUID ply$entityId() {
        Object self = this;
        return self instanceof PlayerEntity ? null : uuid;
    }
}
