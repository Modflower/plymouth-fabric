package net.kjp12.plymouth.locking.mixins;

import net.kjp12.plymouth.locking.ILockable;
import net.kjp12.plymouth.locking.Locking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.kjp12.plymouth.locking.Locking.*;
import static net.minecraft.text.Text.translatable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {
    @Final
    @Shadow
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void helium$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cbir,
                                      BlockState blockState, BlockEntity blockEntity, Block block) {
        if (!Locking.canBreak((ILockable) blockEntity, player)) {
            cbir.setReturnValue(false);
        }
    }

    @Inject(method = "interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;",
                    shift = At.Shift.BEFORE
            )
    )
    private void helium$interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult bhr, CallbackInfoReturnable<ActionResult> cbir,
                                      BlockPos pos, BlockState blockState, boolean bl, boolean bl2) {
        var block = blockState.getBlock();
        if (!blockState.hasBlockEntity()) return;
        var blockEntity = (ILockable) world.getBlockEntity(pos);
        if (blockEntity == null) return;
        var src = player.getCommandSource();
        var lock = Locking.surrogate(world, pos, src);
        if (player.isSneaking()) {
            if (hand == Hand.MAIN_HAND &&
                    isItemNoop(player, player.getStackInHand(Hand.OFF_HAND)) &&
                    isItemNoop(player, player.getStackInHand(Hand.MAIN_HAND))) {
                if (lock.isOwner() || (lock.effective() & PERMISSIONS_BYPASS) != 0) {
                    lock.unclaim();
                    player.sendMessage(translatable("plymouth.locking.unclaimed", toText(block), toText(pos)).formatted(Formatting.YELLOW), true);
                    cbir.setReturnValue(ActionResult.SUCCESS);
                } else if (lock.plymouth$isOwned()) {
                    player.sendMessage(translatable("plymouth.locking.locked", toText(block), lock.getOwner()).formatted(Formatting.RED), true);
                    cbir.setReturnValue(ActionResult.FAIL);
                } else {
                    if (Locking.LOCKING_LOCK_PERMISSION.test(src)) {
                        lock.claim(player.getUuid());
                        player.sendMessage(translatable("plymouth.locking.claimed", toText(block), toText(pos)).formatted(Formatting.GREEN), true);
                        cbir.setReturnValue(ActionResult.SUCCESS);
                    } else {
                        player.sendMessage(translatable("plymouth.locking.denied").formatted(Formatting.RED), true);
                        cbir.setReturnValue(ActionResult.FAIL);
                    }
                }
            } else {
                // TODO: Proper multiblock handling, making them ignore each other if chests and alike.
                if (player.getStackInHand(hand).getItem() instanceof BlockItem blockItem &&
                        lock.plymouth$isOwned() && !lock.isOwner() && blockItem.getBlock() instanceof ChestBlock &&
                        block instanceof ChestBlock && blockState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                    cbir.setReturnValue(ActionResult.FAIL);
                }
            }
        } else if (!lock.canOpen()) {
            cbir.setReturnValue(ActionResult.FAIL);
        }
    }
}
