package gay.ampflower.plymouth.tracker.mixins;

import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AnvilScreenHandler.class)
public abstract class MixinAnvilScreenHandler extends ForgingScreenHandler {
    public MixinAnvilScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    /**
     * Track when the anvil is broken.
     *
     * @author Ampflower
     * @since 0.0.0
     */
    // [RAW ASM - MUST CHECK]
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void plymouth$onTakeOutput$postBreakAnvil(PlayerEntity player, World world, BlockPos pos, CallbackInfo cbir, BlockState oldState, BlockState newState) {
        if (world instanceof ServerWorld)
            DatabaseHelper.database.breakBlock((ServerWorld) world, pos, oldState, null, (Target) player);
    }

    /**
     * Track when the anvil is damaged.
     *
     * @author Ampflower
     * @since 0.0.0
     */
    // [RAW ASM - MUST CHECK]
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void plymouth$onTakeOutput$postDamageAnvil(PlayerEntity player, World world, BlockPos pos, CallbackInfo cbir, BlockState oldState, BlockState newState) {
        if (world instanceof ServerWorld)
            DatabaseHelper.database.replaceBlock((ServerWorld) world, pos, oldState, newState, (Target) player);
    }
}
