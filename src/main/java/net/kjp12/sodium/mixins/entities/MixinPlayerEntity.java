package net.kjp12.sodium.mixins.entities;

import net.kjp12.sodium.SodiumHelper;
import net.kjp12.sodium.helpers.IShadowBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "isBlockBreakingRestricted(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/GameMode;)Z", at = @At("HEAD"), cancellable = true)
    public void sodium$isBlockBreakingRestricted(World world, BlockPos pos, GameMode gameMode, CallbackInfoReturnable<Boolean> cbir) {
        if (!world.isClient && !SodiumHelper.canBreak((IShadowBlockEntity) world.getBlockEntity(pos), this))
            cbir.setReturnValue(true);
    }

    //TODO: Inject into on movement & do 8x4x5 (basically what you're looking at) sending
    // once shadow by lighting has been implemented.
    /*
    @Override
    public void setPos(double x, double y, double z) {
        double i = prevX, j = prevY, k = prevZ;
        super.setPos(x, y, z);
    }*/
}
