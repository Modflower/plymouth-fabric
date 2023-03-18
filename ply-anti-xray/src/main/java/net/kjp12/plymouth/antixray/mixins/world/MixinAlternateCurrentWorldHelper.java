package net.kjp12.plymouth.antixray.mixins.world;// Created 2022-15-07T13:55:09

import net.kjp12.plymouth.antixray.ShadowChunk;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Provides a compatibility layer for Alternate Current's LevelHelper.
 *
 * @author Ampflower
 * @since ${version}
 **/
@Pseudo
@Mixin(targets = "alternate.current.wire.LevelHelper")
public class MixinAlternateCurrentWorldHelper {
    /**
     * Updates the underlying shadow mask if present.
     * <p>
     * If present and not hidden, continues to mark the block for update as normal.
     *
     * @param self The ServerChunkManager being redirected.
     * @param pos  The position being marked for update.
     */
    @Redirect(method = "setWireState(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerChunkManager;markForUpdate(Lnet/minecraft/util/math/BlockPos;)V"))
    private static void plymouth$onMarkForUpdate(ServerChunkManager self, BlockPos pos) {
        int x = ChunkSectionPos.getSectionCoord(pos.getX());
        int z = ChunkSectionPos.getSectionCoord(pos.getZ());
        var c = (ShadowChunk) self.getWorldChunk(x, z);
        if (c != null && c.plymouth$unsafe$uncheckedUpdate(pos)) {
            self.markForUpdate(pos);
        }
    }
}
