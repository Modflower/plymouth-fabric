package net.kjp12.plymouth.tracker.mixins.targets;// Created 2021-13-06T06:39:59

import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.tracker.glue.ITargetInjectable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Shadow
    protected EnderChestInventory enderChestInventory;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void plymouth$injectPlayerIntoEnderChest(CallbackInfo ci) {
        ((ITargetInjectable) enderChestInventory).plymouth$injectTarget((Target) this);
    }
}
