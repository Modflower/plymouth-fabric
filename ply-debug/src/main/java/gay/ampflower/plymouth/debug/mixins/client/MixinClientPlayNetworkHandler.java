package gay.ampflower.plymouth.debug.mixins.client;

import gay.ampflower.plymouth.debug.DebugClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Ampflower
 * @since 0.0.0
 */
@Pseudo
@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void plymouth$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo cbi) {
        DebugClient.onBlockDelta.push(packet.getPos().asLong());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void plymouth$onDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo cbi) {
        packet.visitUpdates((pos, state) -> DebugClient.onBlockDelta.push(pos.asLong()));
    }
}
