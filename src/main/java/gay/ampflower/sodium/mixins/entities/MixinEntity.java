package gay.ampflower.sodium.mixins.entities;

import gay.ampflower.sodium.SodiumHelper;
import gay.ampflower.sodium.helpers.IShadowBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method="canExplosionDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z", at=@At("HEAD"), cancellable = true)
    public void sodium$canExplosionDestroyBlock(Explosion explosion, BlockView blockView, BlockPos pos, BlockState blockState, float power, CallbackInfoReturnable<Boolean> cbir) {
        if (!SodiumHelper.canBreak((IShadowBlockEntity) blockView.getBlockEntity(pos), (Entity) (Object) this))
            cbir.setReturnValue(false);
    }
}
