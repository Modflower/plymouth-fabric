package gay.ampflower.helium.mixins.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hack-fix to attempt to get chunks to reload if they fall into the void for any reason in a bedrock-bottom world.
 * Must apply to worlds that have a bedrock floor (will attempt to detect by position) and are in survival or adventure mode.
 *
 * @author Ampflower
 * @since Dec. 19, 2020 @ 15:36
 **/
@Mixin(ServerPlayerEntity.class)
public abstract class MixinPreventPlayerFallThrough extends PlayerEntity {

    @Shadow
    @Final
    public ServerPlayerInteractionManager interactionManager;

    public MixinPreventPlayerFallThrough(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Shadow
    public abstract ServerWorld getServerWorld();

    @Shadow
    public abstract void sendUnloadChunkPacket(ChunkPos chunkPos);

    @Shadow
    public abstract void sendInitialChunkPackets(ChunkPos chunkPos, Packet<?> packet, Packet<?> packet2);

    @Inject(method = "playerTick()V", at = @At("TAIL"))
    private void onEndPlayerTick(CallbackInfo ci) {
        double x = getX(), y = getY(), z = getZ();
        if (y < 0 && interactionManager.isSurvivalLike()) {
            // We're in survival, fetch the world, chunk then check for bedrock at 0.
            // We're getting the server world as we'll need to directly access TACS and the light manager to force a chunk reload.
            var world = getServerWorld();
            var chunk = world.getChunk(chunkX, chunkZ);

            int ix = MathHelper.fastFloor(x), iz = MathHelper.fastFloor(z);
            if (chunk.getBlockState(new BlockPos(ix, 0, iz)).isOf(Blocks.BEDROCK)) {
                // We are somehow below bedrock, fix position. We'll use this position to fix the player.
                int iy = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(ix & 15, iz & 15);
                // Reset fall damage to avoid accidentally killing the player.
                fallDistance = 0F;
                // Teleport player to the top of the chunk.
                teleport(x, iy + 1, z);
                var chunkManager = world.getChunkManager();
                // Force-send the chunk they're currently in including the lighting.
                // Ensure the chunk is completely unloaded before we reload it.
                sendUnloadChunkPacket(chunk.getPos());
                // Reload the chunk fully.
                sendInitialChunkPackets(chunk.getPos(),
                        new ChunkDataS2CPacket(chunk, 0xFFFF),
                        new LightUpdateS2CPacket(chunk.getPos(), chunkManager.getLightingProvider(), true));
                var tacs = chunkManager.threadedAnvilChunkStorage;
                // Force TACS to update the camera position.
                tacs.updateCameraPosition((ServerPlayerEntity) (Object) this);
            }
        }
    }
}
