package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.database.records.TargetRecord;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements Target {
    @Shadow
    @Nullable
    protected World world;
    @Shadow
    protected BlockPos pos;

    @Shadow
    public abstract BlockState getCachedState();

    @Unique
    private TargetRecord record;

    @Override
    public boolean ply$isBlock() {
        return true;
    }

    @Override
    public TargetRecord plymouth$toRecord() {
        return record == null ? record = Target.super.plymouth$toRecord() : record;
    }

    @Override
    public ServerWorld ply$world() {
        return (ServerWorld) world;
    }

    @Override
    public BlockPos ply$pos3i() {
        return pos;
    }

    // TODO: Implement the method in database helper.
    @Override
    public String ply$name() {
        return null;
    }

    @Override
    public UUID ply$userId() {
        return UUIDHelper.getUUID(getCachedState().getBlock());
    }

    @Override
    public UUID ply$entityId() {
        return null;
    }
}
