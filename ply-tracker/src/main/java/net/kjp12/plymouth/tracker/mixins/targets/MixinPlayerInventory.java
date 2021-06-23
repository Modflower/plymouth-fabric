package net.kjp12.plymouth.tracker.mixins.targets;// Created 2021-13-06T06:26:59

import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.database.records.TargetRecord;
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
 * @author KJP12
 * @since ${version}
 **/
@Mixin(PlayerInventory.class)
public class MixinPlayerInventory<T extends PlayerEntity & Target> implements Target {
    @Shadow
    @Final
    public T player;

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
