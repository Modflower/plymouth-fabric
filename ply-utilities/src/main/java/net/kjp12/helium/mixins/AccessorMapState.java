package net.kjp12.helium.mixins;// Created 2021-02-06T15:27:08

import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(MapState.class)
public interface AccessorMapState {
    @Accessor
    List<MapState.PlayerUpdateTracker> getUpdateTrackers();

    @Invoker
    void callMarkDirty(int x, int y);
}
