package net.kjp12.plymouth.debug.mixins.client;// Created 2021-03-29T01:54:24

import net.kjp12.plymouth.debug.anti_xray.AntiXrayClientDebugger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
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
public class MixinBustAntiXrayClientPlayNetworkHandler {
    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void plymouth$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo cbi) {
        AntiXrayClientDebugger.onBlockDelta.push(packet.getPos().asLong());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void plymouth$onDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo cbi) {
        packet.visitUpdates((pos, state) -> AntiXrayClientDebugger.onBlockDelta.push(pos.asLong()));
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void plymouth$onChunkData(ChunkDataS2CPacket packet, CallbackInfo cbi) {
        AntiXrayClientDebugger.onChunkLoad.push(BlockPos.asLong(packet.getX(), 0, packet.getZ()));
        packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()).accept((pos, $1, $2) ->
                AntiXrayClientDebugger.onChunkBlockEntity.push(pos.asLong()));
    }

    @Inject(method = "onBlockEntityUpdate", at = @At("RETURN"))
    private void plymouth$onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo cbi) {
        AntiXrayClientDebugger.onBlockEntityUpdate.push(packet.getPos().asLong());
    }

    @Inject(method = "onBlockEvent", at = @At("RETURN"))
    private void plymouth$onBlockEvent(BlockEventS2CPacket packet, CallbackInfo cbi) {
        AntiXrayClientDebugger.onBlockEvent.push(packet.getPos().asLong());
    }
}
