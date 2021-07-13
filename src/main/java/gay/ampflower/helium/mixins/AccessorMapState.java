/* Copyright (c) 2021 Ampflower
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package gay.ampflower.helium.mixins;

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
