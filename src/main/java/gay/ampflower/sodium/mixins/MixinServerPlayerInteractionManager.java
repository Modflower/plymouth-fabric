package gay.ampflower.sodium.mixins;

import gay.ampflower.sodium.helpers.IShadowBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {
    @Shadow public ServerPlayerEntity player;

    @Inject(method="tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V",
                    shift = At.Shift.BEFORE
            )
    )
    public void sodium$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cbir,
                                     BlockState blockState, BlockEntity blockEntity, Block block) {
        if (!((IShadowBlockEntity) blockEntity).sodium$canBreakBlock(player)) {
            cbir.setReturnValue(false);
        }
    }

    @Inject(method="interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;",
                    shift = At.Shift.BEFORE
            )
    )
    public void sodium$interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult bhr, CallbackInfoReturnable<ActionResult> cbir,
                                     BlockPos blockPos, BlockState blockState, boolean bl, boolean bl2) {
        var blockEntity = world.getBlockEntity(blockPos);
        if (blockEntity != null && !((IShadowBlockEntity) blockEntity).sodium$canOpenBlock(player)) {
            cbir.setReturnValue(ActionResult.FAIL);
        }
    }
}
