package gay.ampflower.plymouth.anti_xray.mixins.packets.s2c;

import gay.ampflower.plymouth.anti_xray.IShadowChunk;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChunkDataS2CPacket.class)
public class MixinChunkData {
    @Redirect(method = "writeData",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;getSectionArray()[Lnet/minecraft/world/chunk/ChunkSection;"
            ))
    private ChunkSection[] helium$writeData$proxyWorldChunk$getSectionArray(WorldChunk chunk) {
        return ((IShadowChunk) chunk).plymouth$getShadowSections();
    }

    @Redirect(method = "getDataSize",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;getSectionArray()[Lnet/minecraft/world/chunk/ChunkSection;"
            ))
    private ChunkSection[] helium$getDataSize$proxyWorldChunk$getSectionArray(WorldChunk chunk) {
        return ((IShadowChunk) chunk).plymouth$getShadowSections();
    }

    @Redirect(method = "<init>(Lnet/minecraft/world/chunk/WorldChunk;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;getBlockEntities()Ljava/util/Map;"
            ))
    private Map<BlockPos, BlockEntity> helium$init$proxyWorldChunk$getBlockEntities(WorldChunk self) {
        var map = new HashMap<BlockPos, BlockEntity>();
        self.getBlockEntities().forEach((pos, tile) -> {
            if (!((IShadowChunk) self).plymouth$isMasked(pos)) map.put(pos, tile);
        });
        return map;
    }
}
