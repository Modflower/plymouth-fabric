package net.kjp12.plymouth.antixray.mixins.world;// Created 2022-09-02T16:01:32

import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(PalettedContainer.class)
public interface AccessorPalettedContainer<T> {
    @Accessor
    IndexedIterable<T> getIdList();

    @Accessor
    PalettedContainer.PaletteProvider getPaletteProvider();

    @Accessor
    PalettedContainer.Data<T> getData();
}
