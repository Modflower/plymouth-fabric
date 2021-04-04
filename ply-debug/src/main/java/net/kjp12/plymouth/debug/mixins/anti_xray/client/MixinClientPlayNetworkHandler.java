package net.kjp12.plymouth.debug.mixins.anti_xray.client;// Created 2021-03-29T01:54:24

import net.kjp12.plymouth.debug.anti_xray.AntiXrayClientDebugger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author KJP12
 * @since 0.0.0
 */
@Pseudo
@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void plymouth$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo cbi) {
        AntiXrayClientDebugger.onBlockDelta.push(packet.getPos().asLong());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void plymouth$onDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo cbi) {
        packet.visitUpdates((pos, state) -> AntiXrayClientDebugger.onBlockDelta.push(pos.asLong()));
    }
}
