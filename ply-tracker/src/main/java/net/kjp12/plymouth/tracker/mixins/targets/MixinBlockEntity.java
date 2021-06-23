package net.kjp12.plymouth.tracker.mixins.targets;// Created 2021-10-05T22:37:28

import net.kjp12.plymouth.common.UUIDHelper;
import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.database.records.TargetRecord;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * @author KJP12
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
    public World ply$world() {
        return world;
    }

    @Override
    public BlockPos ply$pos3i() {
        return pos;
    }

    @Override
    public String ply$name() {
        return Registry.BLOCK.getId(getCachedState().getBlock()).toString();
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
