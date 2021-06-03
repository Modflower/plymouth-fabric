package gay.ampflower.plymouth.locking.mixins;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.Locking;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
@Mixin(ExplosionBehavior.class)
public class MixinExplosionBehavior {
    @Inject(method = "canDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void helium$canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        if (state.hasBlockEntity() && Locking.canBreak((ILockable) world.getBlockEntity(pos), explosion))
            cir.setReturnValue(false);
    }
}
