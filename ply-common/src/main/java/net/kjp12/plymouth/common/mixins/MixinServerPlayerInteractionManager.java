package net.kjp12.plymouth.common.mixins;// Created 2021-03-27T22:40:45

import net.kjp12.plymouth.common.InjectableInteractionManager;
import net.kjp12.plymouth.common.InteractionManagerInjection;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A manager injector into the interaction manager. Makes perfect sense.
 *
 * @author Ampflower
 * @since 0.0.0
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager implements InjectableInteractionManager {
    @Shadow
    protected ServerPlayerEntity player;
    @Shadow
    protected ServerWorld world;

    @Shadow
    protected abstract void method_41250(BlockPos pos, boolean success, int sequence, String reason);

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

    @Inject(method = "processBlockBreakingAction",
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void plymouthCommon$tryBreakBlock(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
        if (temporaryInjection != null && action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            var result = temporaryInjection.onBreakBlock(player, world, pos, direction);
            if (result != ActionResult.PASS) {
                player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, this.world.getBlockState(pos)));
                method_41250(pos, false, sequence, "intercepted by plymouth");
                ci.cancel();
            }
        }
    }

    @Inject(method = "interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void plymouthCommon$interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult bhr, CallbackInfoReturnable<ActionResult> cbir) {
        if (temporaryInjection != null) {
            var result = temporaryInjection.onInteractBlock(player, (ServerWorld) world, stack, hand, bhr);
            if (ActionResult.PASS != result) {
                cbir.setReturnValue(result);
            }
        }
    }
}
