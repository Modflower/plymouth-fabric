package net.kjp12.helium.mixins.packets.s2c;

import net.kjp12.helium.helpers.IShadowChunk;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkDeltaUpdateS2CPacket.ChunkDeltaRecord.class)
public class MixinChunkDeltaUpdateRecord {
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "<init>(Lnet/minecraft/network/packet/s2c/play/ChunkDeltaUpdateS2CPacket;SLnet/minecraft/world/chunk/WorldChunk;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState helium$init$proxyWorldChunk$getBlockState(WorldChunk self, BlockPos pos) {
        return ((IShadowChunk) self).helium$getShadowBlock(pos);
    }
}
