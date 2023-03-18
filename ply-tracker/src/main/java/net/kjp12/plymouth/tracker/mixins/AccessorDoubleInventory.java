package net.kjp12.plymouth.tracker.mixins;// Created 2021-09-05T16:27:07

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
