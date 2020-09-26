package gay.ampflower.helium.mixins;

import gay.ampflower.helium.Helium;
import gay.ampflower.helium.helpers.IShadowBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
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

import java.util.UUID;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V",
                    shift = At.Shift.BEFORE
            )
    )
    public void helium$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cbir,
                                     BlockState blockState, BlockEntity blockEntity, Block block) {
        if (!((IShadowBlockEntity) blockEntity).helium$canBreakBlock(player)) {
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
    public void helium$interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult bhr, CallbackInfoReturnable<ActionResult> cbir,
                                     BlockPos pos, BlockState blockState, boolean bl, boolean bl2) {
        if(!blockState.getBlock().hasBlockEntity()) return;
        var blockEntity = (IShadowBlockEntity) world.getBlockEntity(pos);
        if(blockEntity == null) return;
        if (!blockEntity.helium$canOpenBlock(player)) {
            cbir.setReturnValue(ActionResult.FAIL);
        } else {
            var otherPos = Helium.getOtherPos(world, pos);
            if (player.isSneaking()) {
                if (player.getStackInHand(hand).isEmpty()) {
                    var obe = otherPos == null ? null : (IShadowBlockEntity) world.getBlockEntity(otherPos);
                    var puid = player.getUuid();
                    if (obe == null) {
                        var buid = blockEntity.helium$getOwner();
                        if (buid == null) {
                            blockEntity.helium$setOwner(puid);
                            player.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            cbir.setReturnValue(ActionResult.SUCCESS);
                        } else if (buid.equals(puid) || player.hasPermissionLevel(2)) {
                            blockEntity.helium$setOwner(null);
                            player.sendMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), true);
                            cbir.setReturnValue(ActionResult.SUCCESS);
                        } else {
                            player.sendMessage(new LiteralText(buid + " already claimed this block!").formatted(Formatting.RED), true);
                            cbir.setReturnValue(ActionResult.FAIL);
                        }
                    } else {
                        UUID oa = blockEntity.helium$getOwner(), ob = obe.helium$getOwner();
                        if (oa == null && ob == null) {
                            blockEntity.helium$setOwner(puid);
                            obe.helium$setOwner(puid);
                            player.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            cbir.setReturnValue(ActionResult.SUCCESS);
                        } else if (puid.equals(oa) || puid.equals(ob) || player.hasPermissionLevel(2)) {
                            blockEntity.helium$setOwner(null);
                            obe.helium$setOwner(null);
                            player.sendMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), true);
                            cbir.setReturnValue(ActionResult.SUCCESS);
                        } else {
                            player.sendMessage(new LiteralText(oa + " already claimed this block!").formatted(Formatting.RED), true);
                            cbir.setReturnValue(ActionResult.FAIL);
                        }
                    }
                } else {
                    var item = player.getStackInHand(hand).getItem();
                    if (item instanceof BlockItem) {
                        var block = ((BlockItem) item).getBlock();
                        if (block instanceof ChestBlock && otherPos == null && blockEntity.helium$getOwner() != null && !blockEntity.helium$isOwner(player.getUuid())) {
                            cbir.setReturnValue(ActionResult.FAIL);
                        }
                    }
                }
            }
        }
    }
}
