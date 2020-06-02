package gay.ampflower.sodium.mixins;

import gay.ampflower.sodium.helpers.IShadowChunk;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage implements ChunkHolder.PlayersWatchingChunkProvider {
    @Shadow
    @Final
    private ServerLightingProvider serverLightingProvider;

    @Inject(method = "sendChunkDataPackets(Lnet/minecraft/server/network/ServerPlayerEntity;[Lnet/minecraft/network/Packet;Lnet/minecraft/world/chunk/WorldChunk;)V", at = @At("HEAD"))
    private void sodium$sendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo cbi) {
        if (packets[0] == null) {
            ((IShadowChunk) chunk).sodium$generateShadow();
        }
    }
}
