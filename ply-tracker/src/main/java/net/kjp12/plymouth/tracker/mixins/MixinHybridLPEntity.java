package net.kjp12.plymouth.tracker.mixins;

import net.kjp12.plymouth.database.DatabaseHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerEntity.class, LivingEntity.class})
public abstract class MixinHybridLPEntity extends Entity {
    public MixinHybridLPEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;onDamage(Lnet/minecraft/entity/damage/DamageSource;FF)V"), require = 1)
    private void helium$onDamage(DamageSource source, float amount, CallbackInfo cbi) {
        if (!world.isClient) DatabaseHelper.database.hurtEntity((LivingEntity) (Object) this, amount, source);
    }
}
