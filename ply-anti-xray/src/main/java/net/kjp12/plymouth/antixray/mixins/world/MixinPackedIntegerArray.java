package net.kjp12.plymouth.antixray.mixins.world;// Created 2021-03-30T18:27:07

import net.kjp12.plymouth.antixray.CloneAccessible;
import net.minecraft.util.collection.PackedIntegerArray;
import org.spongepowered.asm.mixin.*;

/**
 * Forces PackedIntegerArray to be cloneable.
 *
 * @author KJP12
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
