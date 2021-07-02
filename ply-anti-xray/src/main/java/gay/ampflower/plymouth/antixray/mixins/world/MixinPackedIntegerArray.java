package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.CloneAccessible;
import net.minecraft.util.collection.PackedIntegerArray;
import org.spongepowered.asm.mixin.*;

/**
 * Forces PackedIntegerArray to be cloneable.
 *
 * @author Ampflower
 * @since 0.0.0
 */
@Mixin(PackedIntegerArray.class)
public class MixinPackedIntegerArray implements Cloneable, CloneAccessible {
    @Mutable
    @Shadow
    @Final
    private long[] storage;

    @Override
    @Intrinsic
    public PackedIntegerArray clone() {
        try {
            var self = (MixinPackedIntegerArray) super.clone();
            self.storage = self.storage.clone();
            //noinspection ConstantConditions
            return (PackedIntegerArray) (Object) self;
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }
}
