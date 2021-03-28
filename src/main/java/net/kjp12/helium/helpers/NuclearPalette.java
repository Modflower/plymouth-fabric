package net.kjp12.helium.helpers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.world.chunk.Palette;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A more concurrent version of the Mojang palette.
 * <p>
 * Nuclear, because this may explode. Expectation is concurrent access. Reality may be nuked in the process.
 * <p>
 * TODO: Probably better redesign this for use with the atomic paletted container; this implementation is not ideal at all and needs to be shielded from unexpected access.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public class NuclearPalette<T> implements Palette<T> {
    private final IdList<T> idList;
    private final int indexBits;
    private final AtomicPalettedContainer<T> resizeHandler;
    private final Function<CompoundTag, T> elementDeserializer;
    private final Function<T, CompoundTag> elementSerializer;
    private final Int2ObjectBiMap<T> map;

    /**
     * To prevent the palette from entirely exploding.
     * TODO: Decide on if this should be internal (here) or external
     */
    private final StampedLock lock = new StampedLock();

    public NuclearPalette(IdList<T> idList, int indexBits, AtomicPalettedContainer<T> resizeHandler, Function<CompoundTag, T> elementDeserializer, Function<T, CompoundTag> elementSerializer) {
        this.idList = idList;
        this.indexBits = indexBits;
        this.resizeHandler = resizeHandler;
        this.elementDeserializer = elementDeserializer;
        this.elementSerializer = elementSerializer;
        this.map = new Int2ObjectBiMap<>(1 << indexBits);
    }

    @Override
    public int getIndex(T object) {
        // while true will, normally, be eaten
        // this is only here in the event that a write lock fails to be obtained from a read lock
        while (true) {
            long readLock = lock.readLock();
            int index = map.getRawId(object);
            if (index == -1) {
                long writeLock = lock.tryConvertToWriteLock(readLock);
                if (writeLock == 0) {
                    lock.unlockRead(readLock);
                    continue;
                }
                index = map.add(object);
                if (index >= 1 << indexBits) {
                    index = this.resizeHandler.onResize(indexBits + 1, object);
                }
                lock.unlockWrite(writeLock);
                return index;
            }
            lock.unlockRead(readLock);
            return index;
        }
    }

    /**
     * This is an internal call that is synced against PalettedContainer.
     * <p>
     * Do not call this unless you want the map to explode.
     */
    int getIndexDesync(T object) {
        int index = map.getRawId(object);
        return index == -1 ? map.add(object) : index;
    }

    @Override
    public boolean accepts(Predicate<T> predicate) {
        for (int i = 0; i < getIndexBits(); i++) {
            if (predicate.test(map.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public T getByIndex(int index) {
        return map.get(index);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void fromPacket(PacketByteBuf buf) {
        map.clear();
        int i = buf.readVarInt();

        for (int j = 0; j < i; j++) {
            map.add(idList.get(buf.readVarInt()));
        }
    }

    @Override
    public void toPacket(PacketByteBuf buf) {
        int i = getIndexBits();
        buf.writeVarInt(i);
        for (int j = 0; j < i; j++) {
            buf.writeVarInt(idList.getRawId(map.get(j)));
        }
    }

    @Override
    public int getPacketSize() {
        final int i = getIndexBits();
        int j = PacketByteBuf.getVarIntSizeBytes(i);
        for (int k = 0; k < i; k++) {
            j += PacketByteBuf.getVarIntSizeBytes(idList.getRawId(map.get(k)));
        }
        return j;
    }

    public int getIndexBits() {
        return map.size();
    }

    @Override
    public void fromTag(ListTag tag) {
        map.clear();

        for (int i = 0, j = tag.size(); i < j; i++) {
            map.add(elementDeserializer.apply(tag.getCompound(i)));
        }
    }

    public void toTag(ListTag tag) {
        for (int i = 0, j = getIndexBits(); i < j; i++) {
            tag.add(elementSerializer.apply(map.get(i)));
        }
    }
}
