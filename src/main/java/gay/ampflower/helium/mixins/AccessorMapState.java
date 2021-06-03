package gay.ampflower.helium.mixins;

import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(MapState.class)
public interface AccessorMapState {
    @Invoker
    void callMarkDirty(int x, int y);
}
