package gay.ampflower.plymouth.tracker.mixins.targets;

import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.tracker.glue.ITargetInjectable;
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
