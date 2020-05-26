package net.kjp12.sodium.mixins;

import net.kjp12.sodium.helpers.IProtectBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow public abstract UUID getUuid();

    @Inject(method="canExplosionDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z", at=@At("HEAD"), cancellable = true)
    public void sodium$canExplosionDestroyBlock(Explosion explosion, BlockView blockView, BlockPos pos, BlockState blockState, float power, CallbackInfoReturnable<Boolean> cbir) {
        var blockEntity = (IProtectBlock) blockView.getBlockEntity(pos);
        if(blockEntity != null && !blockEntity.sodium$canBreakBlock(getUuid())) cbir.setReturnValue(false);
    }
}
