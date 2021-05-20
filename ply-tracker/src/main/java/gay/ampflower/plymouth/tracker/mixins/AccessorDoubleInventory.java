package gay.ampflower.plymouth.tracker.mixins;

import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(DoubleInventory.class)
public interface AccessorDoubleInventory {
    @Accessor
    Inventory getFirst();

    @Accessor
    Inventory getSecond();
}
