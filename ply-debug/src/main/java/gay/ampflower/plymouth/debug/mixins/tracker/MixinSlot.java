package gay.ampflower.plymouth.debug.mixins.tracker;

import gay.ampflower.plymouth.database.Target;
import gay.ampflower.plymouth.tracker.Tracker;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Ampflower
 * @since 0.0.0
 */
@Mixin(Slot.class)
public class MixinSlot {
    @Shadow
    @Final
    private int index;

    @Shadow
    @Final
    public Inventory inventory;

    @Shadow
    public int id;

    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int y;

    @Inject(method = "onQuickTransfer", at = @At("HEAD"))
    private void plymouth$onStackChanged(ItemStack a, ItemStack b, CallbackInfo cbi) {
        if (inventory instanceof Target target && !target.ply$world().isClient)
            Tracker.logger.info("{} had items changed out: {}, {}", this, a, b, new Throwable());
    }

    @Inject(method = "setStack", at = @At("HEAD"))
    private void plymouth$onSetStack(ItemStack a, CallbackInfo cbi) {
        if (inventory instanceof Target target && !target.ply$world().isClient)
            Tracker.logger.info("{} had items set to {}", this, a, new Throwable());
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"))
    private void plymouth$onSetStack(ItemStack a, CallbackInfoReturnable<ItemStack> cbi) {
        if (inventory instanceof Target target && !target.ply$world().isClient)
            Tracker.logger.info("{} had items set to {}", this, a, new Throwable());
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"))
    private void plymouth$onSetStack(ItemStack a, int i, CallbackInfoReturnable<ItemStack> cbi) {
        if (inventory instanceof Target target && !target.ply$world().isClient)
            Tracker.logger.info("{} had items set to {}", this, a, new Throwable());
    }

    @Override
    public String toString() {
        return this.getClass() + "{index=" + index + ", inventory=" + inventory + ", id=" + id + ", pos=(" + x + ',' + y + "), stack=" + inventory.getStack(index) + "}";
    }
}
