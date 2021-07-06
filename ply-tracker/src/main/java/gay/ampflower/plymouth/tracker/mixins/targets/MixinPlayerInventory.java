package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.database.records.TargetRecord;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Allows tracking of player inventories through invsee and similar commands.
 *
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(PlayerInventory.class)
public class MixinPlayerInventory implements Target {
    @Shadow
    @Final
    public PlayerEntity player;

    @Override
    public TargetRecord plymouth$toRecord() {
        return ((Target) player).plymouth$toRecord();
    }

    @Override
    public boolean ply$isBlock() {
        return ((Target) player).ply$isBlock();
    }

    @Override
    public World ply$world() {
        return ((Target) player).ply$world();
    }

    @Override
    public World ply$blockWorld() {
        return ((Target) player).ply$blockWorld();
    }

    @Override
    public BlockPos ply$pos3i() {
        return ((Target) player).ply$pos3i();
    }

    @Override
    public BlockPos ply$blockPos3i() {
        return ((Target) player).ply$blockPos3i();
    }

    @Override
    public Vec3d ply$pos3d() {
        return ((Target) player).ply$pos3d();
    }

    @Override
    public String ply$name() {
        return ((Target) player).ply$name();
    }

    @Override
    public UUID ply$userId() {
        return ((Target) player).ply$userId();
    }

    @Override
    public UUID ply$entityId() {
        return ((Target) player).ply$entityId();
    }

    @Override
    public boolean ply$targetMatches(@Nullable Target other) {
        return ((Target) player).ply$targetMatches(other);
    }
}
