package net.kjp12.plymouth.tracker.mixins;// Created 2021-28-07T17:36:52

import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.Target;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KJP12
 * @since ${version}
 **/
@Mixin(AbstractDecorationEntity.class)
public abstract class MixinAbstractDecorationEntity extends Entity {
    public MixinAbstractDecorationEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;kill()V"))
    private void plymouth$onKill(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isClient) DatabaseHelper.database.killEntity((Target) this, (Target) source);
    }
}
