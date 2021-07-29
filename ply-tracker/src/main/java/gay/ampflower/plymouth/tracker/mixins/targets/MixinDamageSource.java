package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(DamageSource.class)
public class MixinDamageSource implements Target {
    @Override
    public World ply$world() {
        var cause = UUIDHelper.getEntity((DamageSource) (Object) this);
        return cause != null ? cause.world : null;
    }

    @Override
    public BlockPos ply$pos3i() {
        var cause = UUIDHelper.getEntity((DamageSource) (Object) this);
        return cause != null ? cause.getBlockPos() : null;
    }

    @Override
    public Vec3d ply$pos3d() {
        var cause = UUIDHelper.getEntity((DamageSource) (Object) this);
        return cause != null ? cause.getPos() : null;
    }

    @Override
    public String ply$name() {
        return DatabaseHelper.getName((DamageSource) (Object) this);
    }

    @Override
    public UUID ply$userId() {
        return UUIDHelper.getUUID((DamageSource) (Object) this);
    }

    @Override
    public UUID ply$entityId() {
        var cause = UUIDHelper.getEntity((DamageSource) (Object) this);
        return cause instanceof PlayerEntity ? null : cause.getUuid();
    }
}
