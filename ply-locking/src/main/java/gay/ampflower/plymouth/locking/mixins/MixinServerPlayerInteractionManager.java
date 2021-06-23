package gay.ampflower.plymouth.locking.mixins;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.Locking;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;
import java.util.UUID;

import static gay.ampflower.plymouth.locking.Locking.toText;

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
        if (!blockEntity.helium$canOpenBlock(player)) {
            cbir.setReturnValue(ActionResult.FAIL);
        } else if (player.isSneaking()) {
            var src = player.getCommandSource();
            // I don't expect people to have more than two hands, but if feet becomes a replacement for hands, this is ready.
            var otherPos = Locking.getOtherPos(world, pos);
            if (hand == Hand.MAIN_HAND &&
                    player.getStackInHand(Hand.OFF_HAND).isEmpty() &&
                    player.getStackInHand(Hand.MAIN_HAND).isEmpty() &&
                    Locking.LOCKING_LOCK_PERMISSION.test(src)) {
                var obe = otherPos == null ? null : (ILockable) world.getBlockEntity(otherPos);
                var puid = player.getUuid();
                if (obe == null) {
                    var handler = blockEntity.plymouth$getPermissionHandler();
                    if (handler == null) {
                        setClaimed(player, cbir, pos, block, blockEntity, puid);
                    } else  // note for the check: If you own this block, we're assuming that you have sufficient permission to disown it.
                        // Admins or operators can however override.
                        if (handler.isOwner(puid) || Locking.LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(src)) {
                            setUnclaimed(player, cbir, pos, block, blockEntity);
                        } else {
                            player.sendMessage(new LiteralText(handler.getOwner() + " already claimed this block!").formatted(Formatting.RED), true);
                            cbir.setReturnValue(ActionResult.FAIL);
                        }
                } else {
                    IPermissionHandler ha = blockEntity.plymouth$getPermissionHandler(), hb = obe.plymouth$getPermissionHandler();
                    if (ha == null && hb == null) {
                        blockEntity.helium$setOwner(puid);
                        setClaimed(player, cbir, pos, block, obe, puid);
                    } else if (isOwner(ha, puid) || isOwner(hb, puid) || Locking.LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(src)) {
                        blockEntity.helium$setOwner(null);
                        setUnclaimed(player, cbir, pos, block, obe);
                    } else {
                        player.sendMessage(new LiteralText(Objects.requireNonNullElse(ha, hb).getOwner() + " already claimed !").formatted(Formatting.RED), true);
                        cbir.setReturnValue(ActionResult.FAIL);
                    }
                }
            } else {
                var item = player.getStackInHand(hand).getItem();
                if (item instanceof BlockItem) {
                    var blockInHand = ((BlockItem) item).getBlock();
                    if (blockInHand instanceof ChestBlock && otherPos == null && isNotNullAndNotOwner(blockEntity.plymouth$getPermissionHandler(), player.getUuid())) {
                        cbir.setReturnValue(ActionResult.FAIL);
                    }
                }
            }
        }
    }

    @Unique
    private static void setClaimed(ServerPlayerEntity player, CallbackInfoReturnable<ActionResult> cbir, BlockPos pos, Block block, ILockable blockEntity, UUID puid) {
        blockEntity.helium$setOwner(puid);
        player.sendMessage(new TranslatableText("plymouth.locking.claimed", new TranslatableText(block.getTranslationKey()).formatted(Formatting.AQUA), toText(pos)).formatted(Formatting.GREEN), true);
        cbir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private static void setUnclaimed(ServerPlayerEntity player, CallbackInfoReturnable<ActionResult> cbir, BlockPos pos, Block block, ILockable blockEntity) {
        blockEntity.helium$setOwner(null);
        player.sendMessage(new TranslatableText("plymouth.locking.unclaimed", new TranslatableText(block.getTranslationKey()).formatted(Formatting.AQUA), toText(pos)).formatted(Formatting.YELLOW), true);
        cbir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private static boolean isOwner(IPermissionHandler handler, UUID check) {
        return handler != null && handler.isOwner(check);
    }

    @Unique
    private static boolean isNotNullAndNotOwner(IPermissionHandler handler, UUID check) {
        return handler != null && !handler.isOwner(check);
    }
}
