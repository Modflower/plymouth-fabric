package gay.ampflower.sodium.mixins;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow public abstract BlockEntity getBlockEntity(BlockPos pos);

    // This is never called.
    /*
    @Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getFluidState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/fluid/FluidState;",
                    shift = At.Shift.BEFORE
            )
    )
    public void sodium$breakBlock(BlockPos pos, boolean drop, Entity breaker, CallbackInfoReturnable<Boolean> cbir) {
        System.out.println(pos);
        var blockEntity = (IProtectBlock) getBlockEntity(pos);
        if(blockEntity != null) {
            breakPoint();
            if(blockEntity.sodium$getOwner() != null) {
                if(!blockEntity.sodium$canBreakBlock(
                        breaker == null ? null :
                                breaker instanceof TntEntity ? ((TntEntity) breaker).getCausingEntity().getUuid() :
                                        breaker.getUuid())) cbir.setReturnValue(false);
            }
        }
    }
     */
}
