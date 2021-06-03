package gay.ampflower.plymouth.anti_xray.mixins.player;

import gay.ampflower.plymouth.anti_xray.IShadowChunk;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * You know, I really shouldn't have to, but people seem to really like abusing composters.
 *
 * @author Ampflower
 * @since 0.0.0
 **/
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "updatePose()V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityPose;SWIMMING:Lnet/minecraft/entity/EntityPose;", ordinal = 2))
    private void helium$onUpdateSize(CallbackInfo ci) {
        // We need access to shadow immediately to force shadow replacement with spruce planks.
        var self = getBlockPos();
        var chunk = (IShadowChunk) world.getChunk(self);
        if (chunk.plymouth$isMasked(self)) return;
        var possibleComposter = chunk.plymouth$getShadowBlock(self);
        if (possibleComposter.isOf(Blocks.COMPOSTER)) {
            var abovePosition = self.up();
            var aboveBlock = chunk.plymouth$getShadowBlock(abovePosition);
            if (chunk.plymouth$isCulling(aboveBlock, Direction.DOWN, abovePosition)) {
                chunk.plymouth$setShadowBlock(self, Blocks.SPRUCE_PLANKS.getDefaultState());
            }
        }
    }
}
