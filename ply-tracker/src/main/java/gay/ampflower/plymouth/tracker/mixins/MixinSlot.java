package gay.ampflower.plymouth.tracker.mixins;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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

//    @Inject(method = "onStackChanged", at = @At("HEAD"))
//    private void plymouth$onStackChanged(ItemStack a, ItemStack b, CallbackInfo cbi) {
//        if(inventory instanceof BlockEntity && !((BlockEntity) inventory).getWorld().isClient)
//        Tracker.logger.info("{} had items changed out: {}, {}", this, a, b, new Throwable());
//    }
//
//    @Inject(method = "setStack", at = @At("HEAD"))
//    private void plymouth$onSetStack(ItemStack a, CallbackInfo cbi) {
//        if(inventory instanceof BlockEntity && !((BlockEntity) inventory).getWorld().isClient)
//        Tracker.logger.info("{} had items set to {}", this, a, new Throwable());
//    }

    @Override
    public String toString() {
        return this.getClass() + "{index=" + index + ", inventory=" + inventory + ", id=" + id + ", pos=(" + x + ',' + y + "), stack=" + inventory.getStack(index) + "}";
    }
}
