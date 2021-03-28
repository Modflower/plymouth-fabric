package gay.ampflower.plymouth.locking.mixins.entities;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.Locking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntMinecartEntity.class)
public abstract class MixinTntMinecartEntity extends AbstractMinecartEntity {
    private MixinTntMinecartEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "canExplosionDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z", at = @At("HEAD"), cancellable = true)
    private void helium$canExplosionDestroyBlock(Explosion explosion, BlockView blockView, BlockPos pos, BlockState blockState, float power, CallbackInfoReturnable<Boolean> cbir) {
        if (blockState.getBlock().hasBlockEntity() && !Locking.canBreak((ILockable) blockView.getBlockEntity(pos), this))
            cbir.setReturnValue(false);
    }
}
