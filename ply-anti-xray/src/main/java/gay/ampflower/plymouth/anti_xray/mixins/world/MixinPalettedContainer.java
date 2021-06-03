package gay.ampflower.plymouth.anti_xray.mixins.world;

import gay.ampflower.plymouth.anti_xray.CloneAccessible;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.IdListPalette;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.Semaphore;

/**
 * Forces PalettedContainer to be cloneable.
 *
 * @author Ampflower
 * @since 0.0.0
 */
@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements Cloneable, CloneAccessible {
    @Shadow
    private Palette<T> palette;

    @Shadow
    public abstract void lock();

    // We're purposely mutating the writeLock within the clone method.
    @Shadow
    @Final
    @Mutable
    private Semaphore writeLock;

    @Shadow
    public abstract void unlock();

    @Shadow
    protected PackedIntegerArray data;

    @Shadow
    public abstract int onResize(int i, T object);

    @Shadow
    private int paletteSize;

    @Shadow
    @Final
    private T defaultValue;

    @SuppressWarnings("unchecked")
    @Override
    @Intrinsic
    public PalettedContainer<T> clone() {
        // We need to make sure that the chunk section doesn't get modified while this is getting cloned.
        lock();
        try {
            var self = (MixinPalettedContainer<T>) super.clone();
            // We need to make a new write lock for as this is a new chunk section.
            self.writeLock = new Semaphore(1);
            // The IdListPalette is perfectly safe to leave untouched, for as it is immutable.
            if (!(self.palette instanceof IdListPalette)) {
                // Force a null value for the purpose of forced "resize"
                self.paletteSize = 0;
                self.onResize(paletteSize, defaultValue);
                if (self.data == data || self.palette == palette)
                    throw new AssertionError("Data or palette was not regenerated!");
            } else {
                self.data = (PackedIntegerArray) ((CloneAccessible) self.data).clone();
            }
            //noinspection ConstantConditions
            return (PalettedContainer<T>) (Object) self;
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        } finally {
            unlock();
        }
    }
}
