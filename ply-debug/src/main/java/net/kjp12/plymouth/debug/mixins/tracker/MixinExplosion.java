package net.kjp12.plymouth.debug.mixins.tracker;// Created 2021-11-06T23:52:53

import net.kjp12.plymouth.debug.Debug;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs if an explosion is missing an entity if it's destructive.
 *
 * @author KJP12
 * @since ${version}
 **/
@Mixin(Explosion.class)
public class MixinExplosion {
    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)V", at = @At("RETURN"))
    private void plymouth$logIfMissingEntity(World world, Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType, CallbackInfo ci) {
        if (entity == null && destructionType != Explosion.DestructionType.NONE)
            Debug.logger.warn("Missing entity for given destructive explosion {world={}, x={}, y={}, z={}, p={}, fire={}, destruction={}}", world, x, y, z, power, createFire, destructionType, new Throwable("Stack trace."));
    }
}
