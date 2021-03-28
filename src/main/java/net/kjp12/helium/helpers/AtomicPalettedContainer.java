package net.kjp12.helium.helpers;

import net.kjp12.helium.Helium;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Palette;

import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * An atomic paletted container, for used with multi-threaded read and writes.
 * This differs from the vanilla paletted container by using a stamped lock for concurrent access, but also an exclusive hold for resizing or extreme rewriting of the palette.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class AtomicPalettedContainer<T> {
    // this palette is RO, we don't need to worry about concurrent access.
    private final Palette<T> fallbackPalette;
    private final IdList<T> idList;
    private final Function<CompoundTag, T> elementDeserializer;
    private final Function<T, CompoundTag> elementSerializer;
    private final T defaultValue;
    protected AtomicPackedIntegerArray data;
    // this one, we need to guarantee atomic access, else it will be *unhappy*
    private Palette<T> palette;
    private int paletteSize;

    /**
     * This is the primary difference between this and the vanilla version.
     * <p>
     * Do note, this is a partial misuse, using the read side for regular read and write,
     * while the write is an exclusive hold, usually for a palette resize.
     */
    private final StampedLock lock = new StampedLock();

    public AtomicPalettedContainer(Palette<T> fallbackPalette, IdList<T> idList, Function<CompoundTag, T> elementDeserializer, Function<T, CompoundTag> elementSerializer, T defaultValue) {
        this.fallbackPalette = fallbackPalette;
        this.idList = idList;
        this.elementDeserializer = elementDeserializer;
        this.elementSerializer = elementSerializer;
        this.defaultValue = defaultValue;
        this.setPaletteSize(4);
    }

    private void setPaletteSize(int size) {
        if (this.paletteSize != size) {
            if (size < 9) {
                this.paletteSize = Math.max(size, 4);
                this.palette = new NuclearPalette<>(this.idList, this.paletteSize, this, this.elementDeserializer, this.elementSerializer);
                // this.palette = new BiMapPalette<T>(this.idList, this.paletteSize, this, this.elementDeserializer, this.elementSerializer);
            } else {
                this.palette = this.fallbackPalette;
                this.paletteSize = MathHelper.log2DeBruijn(this.idList.size());
            }

            this.palette.getIndex(this.defaultValue);
            this.data = new AtomicPackedIntegerArray(this.paletteSize, 4096);
        }
    }

    public int onResize(int i, T object) {
        // This is a reentrant method; a different method must be considered.
        long writeLock = lock.writeLock();
        var data = this.data;
        var palette = this.palette;
        this.setPaletteSize(i);

        if (this.palette instanceof NuclearPalette) {
            // This code can easily become hot if we don't explicitly check for the nuclear palette
            // We're bypassing the lock that would otherwise be in place, as we're on an exclusive hold.
            var nuclearPalette = (NuclearPalette<T>) this.palette;
            for (int j = 0; j < 4096; j++) {
                T t2 = palette.getByIndex(data.get(j));
                if (t2 != null) data.set(j, nuclearPalette.getIndexDesync(t2));
            }
        } else {
            for (int j = 0; j < 4096; j++) {
                T t2 = palette.getByIndex(data.get(j));
                if (t2 != null) data.set(j, this.palette.getIndex(t2));
            }
        }

        lock.unlockWrite(writeLock);
        return this.palette.getIndex(object);
    }

    public T setSync(int x, int y, int z, T t) {
        long readLock = lock.readLock();
        T t2 = setAndGetOldValue(Helium.toChunkIndex(x, y, z), t);
        lock.unlockRead(readLock);
        return t2;
    }

    public T set(int x, int y, int z, T t) {
        // we cannot desync read/write; unsafe operations
        return setSync(x, y, z, t);
    }

    protected T setAndGetOldValue(int index, T value) {
        return Objects.requireNonNullElse(palette.getByIndex(data.setAndGetOldValue(index, palette.getIndex(value))), defaultValue);
    }

    public T get(int x, int y, int z) {
        return get(Helium.toChunkIndex(x, y, z));
    }

    protected T get(int index) {
        long readLock = lock.readLock();
        T t = Objects.requireNonNullElse(palette.getByIndex(data.get(index)), defaultValue);
        lock.unlockRead(readLock);
        return t;
    }
}
