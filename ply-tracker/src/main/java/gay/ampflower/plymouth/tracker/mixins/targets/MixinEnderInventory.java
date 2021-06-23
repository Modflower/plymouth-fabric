package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.database.records.TargetRecord;
import gay.ampflower.plymouth.tracker.glue.ITargetInjectable;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(EnderChestInventory.class)
public class MixinEnderInventory implements Target, ITargetInjectable {
    @Unique
    public Target player;

    public void plymouth$injectTarget(Target player) {
        this.player = player;
    }

    @Override
    public TargetRecord plymouth$toRecord() {
        return player.plymouth$toRecord();
    }

    @Override
    public boolean ply$isBlock() {
        return player.ply$isBlock();
    }

    @Override
    public World ply$world() {
        return player.ply$world();
    }

    @Override
    public World ply$blockWorld() {
        return player.ply$blockWorld();
    }

    @Override
    public BlockPos ply$pos3i() {
        return player.ply$pos3i();
    }

    @Override
    public BlockPos ply$blockPos3i() {
        return player.ply$blockPos3i();
    }

    @Override
    public Vec3d ply$pos3d() {
        return player.ply$pos3d();
    }

    @Override
    public String ply$name() {
        return player.ply$name();
    }

    @Override
    public UUID ply$userId() {
        return player.ply$userId();
    }

    @Override
    public UUID ply$entityId() {
        return player.ply$entityId();
    }

    @Override
    public boolean ply$targetMatches(@Nullable Target other) {
        return player.ply$targetMatches(other);
    }
}
