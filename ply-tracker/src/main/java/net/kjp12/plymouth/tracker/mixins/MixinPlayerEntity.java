package net.kjp12.plymouth.tracker.mixins;

import net.kjp12.plymouth.database.DatabaseHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author Ampflower
 * @since Jan. 02, 2021 @ 15:43
 **/
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends Entity {
    public MixinPlayerEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At(value = "RETURN", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    private void helium$onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir, double ignored$0, ItemEntity entity) {
        DatabaseHelper.database.createEntity(entity, this);
    }
}
