package net.kjp12.plymouth.locking.mixins.entities;

import net.kjp12.plymouth.locking.ILockable;
import net.kjp12.plymouth.locking.Locking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    private MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "isBlockBreakingRestricted(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/GameMode;)Z", at = @At("HEAD"), cancellable = true)
    private void helium$isBlockBreakingRestricted(World world, BlockPos pos, GameMode gameMode, CallbackInfoReturnable<Boolean> cbir) {
        if (!world.isClient && !Locking.canBreak((ILockable) world.getBlockEntity(pos), this))
            cbir.setReturnValue(true);
    }
}
