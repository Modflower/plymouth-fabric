package gay.ampflower.plymouth.tracker.mixins;

import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinPlayerInteractionManager {
    @Shadow
    public ServerWorld world;

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void plymouth$tryBreakBlock$onBlockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> cbir, BlockState state, BlockEntity entity, Block block) {
        DatabaseHelper.database.breakBlock(world, pos, state, entity == null ? null : entity.toTag(new CompoundTag()), (Target) player);
    }
}
