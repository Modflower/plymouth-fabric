package net.kjp12.plymouth.common.mixins;// Created 2021-03-27T22:40:45

import net.kjp12.plymouth.common.InjectableInteractionManager;
import net.kjp12.plymouth.common.InteractionManagerInjection;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A manager injector into the interaction manager. Makes perfect sense.
 *
 * @author KJP12
 * @since 0.0.0
 */
@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager implements InjectableInteractionManager {
    @Unique
    private InteractionManagerInjection temporaryInjection;


    @Override
    public void setManager(InteractionManagerInjection manager) {
        // The following insertion is purely for debugging in case two managers get set at once. This allows easier inspection by simply enabling assertions.
        assert temporaryInjection == null;
        temporaryInjection = manager;
    }

    @Override
    public InteractionManagerInjection getManager() {
        return temporaryInjection;
    }

    @Inject(method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            cancellable = true,
            at = @At(value = "HEAD"
            )
    )
    private void plymouthCommon$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cbir) {
        if (temporaryInjection != null) {
            var result = temporaryInjection.onBreakBlock(pos);
            if (result != ActionResult.PASS) {
                cbir.setReturnValue(result == ActionResult.SUCCESS);
            }
        }
    }

    @Inject(method = "interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void plymouthCommon$interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult bhr, CallbackInfoReturnable<ActionResult> cbir) {
        if (temporaryInjection != null) {
            var result = temporaryInjection.onInteractBlock(player, world, stack, hand, bhr);
            if (ActionResult.PASS != result) {
                cbir.setReturnValue(result);
            }
        }
    }
}
