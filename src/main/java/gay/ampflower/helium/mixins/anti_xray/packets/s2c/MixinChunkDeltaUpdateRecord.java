package gay.ampflower.helium.mixins.anti_xray.packets.s2c;

import gay.ampflower.helium.helpers.IShadowChunk;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkHolder.class)
public class MixinChunkDeltaUpdateRecord {
    @Redirect(method = "flushUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;getSectionArray()[Lnet/minecraft/world/chunk/ChunkSection;"))
    private ChunkSection[] helium$init$proxyWorldChunk$getBlockState(WorldChunk self) {
        return ((IShadowChunk) self).helium$getShadowSections();
    }
}
