package net.kjp12.helium.mixins.world;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld {
    /**
     * Null Router for {@link ServerChunkManager#markForUpdate(BlockPos)} at {@link ServerWorld#updateListeners(BlockPos, BlockState, BlockState, int)}
     *
     * @reason We're doing our own logic within the shadow chunks. We don't need the world to send
     * updates against what the shadow holds. This'll help avoid network overhead in the process
     * as the shadow chunks will also suppress needless updates where applicable (ie, shadow is already shadowed).
     * We'll call this method ourselves when the mask changes.
     * The rest of the method is safe as it doesn't apply to what we are doing.
     */
    @Redirect(method = "updateListeners(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;markForUpdate(Lnet/minecraft/util/math/BlockPos;)V", ordinal = 0))
    public void helium$updateListeners$nullRoute$markForUpdate(ServerChunkManager self, BlockPos pos) {
    }
}
