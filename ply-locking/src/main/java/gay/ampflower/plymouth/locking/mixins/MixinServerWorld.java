package gay.ampflower.plymouth.locking.mixins;

import gay.ampflower.plymouth.locking.ILockable;
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
 * @author Ampflower
 * @since ${version}
 **/
@Mixin({ServerWorld.class, World.class})
public abstract class MixinServerWorld implements WorldAccess {
    /**
     * @author Ampflower
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
