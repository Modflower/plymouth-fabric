package net.kjp12.helium.mixins.world;

import net.kjp12.helium.Helium;
import net.kjp12.helium.helpers.IShadowBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow
    public abstract BlockEntity getBlockEntity(BlockPos pos);

    /**
     * Break Block, dual-purpose injection.
     * <p>
     * - Prevention of breaking owned tile entities.
     * - Logging of broken blocks if succeeds.
     */
    @Inject(method = "method_30093(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getFluidState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/fluid/FluidState;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    public void helium$breakBlock(BlockPos pos, boolean drop, Entity breaker, int i, CallbackInfoReturnable<Boolean> cbir, BlockState blockState) {
        if (blockState.getBlock().hasBlockEntity() && !Helium.canBreak((IShadowBlockEntity) getBlockEntity(pos), breaker)) {
            cbir.setReturnValue(false);
        } else {
            Helium.database.replaceBlock(pos, blockState, Blocks.AIR.getDefaultState(), breaker);
        }
    }
}
