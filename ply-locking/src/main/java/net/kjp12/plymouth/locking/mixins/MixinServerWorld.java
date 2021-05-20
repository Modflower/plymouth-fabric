package net.kjp12.plymouth.locking.mixins;// Created 2021-20-04T06:09:14

import net.kjp12.plymouth.locking.ILockable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KJP12
 * @since ${version}
 **/
@Mixin({ServerWorld.class, World.class})
public abstract class MixinServerWorld implements WorldAccess {
    /**
     * @author KJP12
     * @reason Adds a second layer to ensure that locking is effective.
     */
    @SuppressWarnings("ConstantConditions") // the permission handler should be not null if owned.
    @Inject(method = "canPlayerModifyAt", at = @At("RETURN"), cancellable = true)
    public void plymouth$canPlayerModifyAt(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            var be = (ILockable) getBlockEntity(pos);
            if (be != null && be.plymouth$isOwned() && !be.plymouth$getPermissionHandler().allowDelete(player))
                cir.setReturnValue(Boolean.FALSE);
        }
    }
}
