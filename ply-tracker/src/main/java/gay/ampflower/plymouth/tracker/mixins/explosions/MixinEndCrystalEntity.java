package gay.ampflower.plymouth.tracker.mixins.explosions;

import gay.ampflower.plymouth.common.UUIDHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(EndCrystalEntity.class)
public class MixinEndCrystalEntity {
    @Redirect(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/explosion/Explosion$DestructionType;)Lnet/minecraft/world/explosion/Explosion;"))
    private Explosion plymouth$redirect$world$createExplosion(World world, Entity entity, double x, double y, double z, float power, Explosion.DestructionType destructionType, DamageSource source) {
        return world.createExplosion(UUIDHelper.getEntity(source), x, y, z, power, destructionType);
    }
}
