package gay.ampflower.plymouth.tracker.mixins.explosions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(FireballEntity.class)
public abstract class MixinFireballEntity extends Entity {
    public MixinFireballEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * Fixes critical crash with null being passed for entity.
     */
    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)Lnet/minecraft/world/explosion/Explosion;"))
    private Explosion plymouth$redirect$world$createExplosion(World world, Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType) {
        return world.createExplosion(this, x, y, z, power, createFire, destructionType);
    }
}
