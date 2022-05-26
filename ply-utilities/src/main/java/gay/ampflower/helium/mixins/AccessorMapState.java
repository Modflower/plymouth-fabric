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
