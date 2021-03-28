package net.kjp12.plymouth.anti_xray.mixins.packets.s2c;

import net.kjp12.plymouth.anti_xray.IShadowChunk;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder {
    @Redirect(method = "flushUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState helium$proxyWorld$getBlockState(World self, BlockPos pos) {
        return ((IShadowChunk) self.getChunk(pos.getX() >> 4, pos.getZ() >> 4)).plymouth$getShadowBlock(pos);
    }

    @Redirect(method = "flushUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;getSectionArray()[Lnet/minecraft/world/chunk/ChunkSection;"))
    private ChunkSection[] helium$proxyWorldChunk$getBlockState(WorldChunk self) {
        return ((IShadowChunk) self).plymouth$getShadowSections();
    }
}
