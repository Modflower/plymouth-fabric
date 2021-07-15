package net.kjp12.plymouth.debug.mixins.anti_xray.client;// Created 2021-03-29T01:54:24

import net.kjp12.plymouth.debug.anti_xray.AntiXrayClientDebugger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
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
    @Shadow
    private ClientWorld world;

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
        int yoff = world.getBottomSectionCoord();
        var bits = packet.getVerticalStripBitmask();
        for (int i = 0, l = bits.length(); i < l; i++)
            if (bits.get(i))
                AntiXrayClientDebugger.onChunkLoad.push(BlockPos.asLong(packet.getX(), i + yoff, packet.getZ()));
        for (var entity : packet.getBlockEntityTagList())
            AntiXrayClientDebugger.onChunkBlockEntity.push(BlockPos.asLong(entity.getInt("x"), entity.getInt("y"), entity.getInt("z")));
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
