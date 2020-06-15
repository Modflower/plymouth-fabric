package net.kjp12.sodium.mixins.world;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {
    protected MixinServerWorld(MutableWorldProperties mutableWorldProperties, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, dimensionType, supplier, bl, bl2, l);
    }

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
    public void sodium$updateListeners$nullRoute$markForUpdate(ServerChunkManager self, BlockPos pos) {
    }
}
