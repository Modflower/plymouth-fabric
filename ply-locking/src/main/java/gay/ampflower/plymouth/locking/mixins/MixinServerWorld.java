package gay.ampflower.plymouth.locking.mixins;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gay.ampflower.plymouth.locking.Locking.toText;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin({ServerWorld.class, World.class})
public abstract class MixinServerWorld implements WorldAccess {
    /**
     * @author Ampflower
     * @reason Adds a second layer to ensure that locking is effective.
     */
    @SuppressWarnings("ConstantConditions") // the permission handler should be not null if owned.
    @Inject(method = "canPlayerModifyAt", at = @At("RETURN"), cancellable = true)
    public void plymouth$canPlayerModifyAt(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            var be = (ILockable) getBlockEntity(pos);
            IPermissionHandler handler;
            if (be != null && be.plymouth$isOwned() && !(handler = be.plymouth$getPermissionHandler()).hasAnyPermissions(player.getCommandSource())) {
                player.sendMessage(Text.translatable("plymouth.locking.locked", toText(getBlockState(pos).getBlock()), handler.getOwner()).formatted(Formatting.RED), true);
                cir.setReturnValue(Boolean.FALSE);
            }
        }
    }
}
