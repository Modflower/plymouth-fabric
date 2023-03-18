package net.kjp12.plymouth.tracker.mixins.explosions;// Created 2021-11-06T23:34:11

import net.kjp12.plymouth.tracker.glue.RespawnAnchorExplosionBehavior;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(RespawnAnchorBlock.class)
public abstract class MixinRespawnAnchorBlock {
    @Shadow
    private static boolean hasStillWater(BlockPos pos, World world) {
        return false;
    }

    /**
     * @vanilla-copy
     */
    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RespawnAnchorBlock;explode(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    private void plymouth$redirect$self$explode(RespawnAnchorBlock respawnAnchorBlock, BlockState state, World world, BlockPos explodedPos, BlockState $0, World $1, BlockPos $2, PlayerEntity player) {
        world.removeBlock(explodedPos, false);
        final boolean bl2 = world.getFluidState(explodedPos.up()).isIn(FluidTags.WATER) || Direction.Type.HORIZONTAL.stream().map(explodedPos::offset).anyMatch(pos -> hasStillWater(pos, world));
        world.createExplosion(player, DamageSource.badRespawnPoint(), new RespawnAnchorExplosionBehavior(explodedPos, bl2), explodedPos.getX() + 0.5D, explodedPos.getY() + 0.5D, explodedPos.getZ() + 0.5D, 5.0F, true, Explosion.DestructionType.DESTROY);

    }
}
