package gay.ampflower.helium.mixins.entities;

import gay.ampflower.helium.helpers.IShadowBlockEntity;
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

    public MixinTntMinecartEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "canExplosionDestroyBlock(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;F)Z", at = @At("HEAD"), cancellable = true)
    public void helium$canExplosionDestroyBlock(Explosion explosion, BlockView blockView, BlockPos pos, BlockState blockState, float power, CallbackInfoReturnable<Boolean> cbir) {
        var blockEntity = (IShadowBlockEntity) blockView.getBlockEntity(pos);
        if (blockEntity != null && !blockEntity.helium$canBreakBlock(getUuid())) cbir.setReturnValue(false);
    }
}
