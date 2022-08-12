package gay.ampflower.plymouth.antixray.mixins.world;

import gay.ampflower.plymouth.antixray.Constants;
import gay.ampflower.plymouth.antixray.LazyChunkManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixins a lazy implementation of getChunk meant for environments where the
 * old engine wasn't suited for.
 *
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager extends ChunkManager implements LazyChunkManager {


    @Shadow
    @Nullable
    protected abstract ChunkHolder getChunkHolder(long pos);

    @Shadow
    @Final
    private Chunk[] chunkCache;

    @Shadow
    public abstract @Nullable Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create);

    @Shadow
    @Final
    private long[] chunkPosCache;

    @Unique
    private final long[] lazyChunkPosCache = new long[4];
    @Unique
    private final Chunk[] lazyChunkCache = new Chunk[4];
    @Unique
    private int lazyChunkIndex;

    @Override
    public Chunk plymouth$getChunkLazy(int chunkX, int chunkZ) {
        long chunkPos = ChunkPos.toLong(chunkX, chunkZ);

        // Prefer the completed cache before falling back to lazy.
        for (int i = 0, l = chunkPosCache.length; i < l; i++) {
            if (chunkPosCache[i] == chunkPos && chunkCache[i] != null) {
                return chunkCache[i];
            }
        }

        for (int i = 0, l = lazyChunkPosCache.length; i < l; i++) {
            if (lazyChunkPosCache[i] == chunkPos && lazyChunkCache[i] != null) {
                return lazyChunkCache[i];
            }
        }

        var holder = getChunkHolder(chunkPos);
        if (holder == null) {
            Constants.LOGGER.warn("Missed holder for {}, {}, falling back to getChunk.", chunkX, chunkZ);
            return getChunk(chunkX, chunkZ, ChunkStatus.LIGHT, false);
        }
        var chunk = holder.getCurrentChunk();
        if (chunk == null) {
            Constants.LOGGER.warn("Missed chunk for {}, {}, falling back to getChunk. Got holder {}", chunkX, chunkZ, holder);
            return getChunk(chunkX, chunkZ, ChunkStatus.LIGHT, false);
        } else potato:{
            int i = lazyChunkIndex++ & 3;
            lazyChunkCache[i] = chunk;
            lazyChunkPosCache[i] = chunkPos;
            for (var section : chunk.getSectionArray()) {
                if (!section.isEmpty()) {
                    break potato;
                }
            }
            Constants.LOGGER.warn("Suspiciously empty chunk @ {}, {}: {}", chunkX, chunkZ, chunk);
        }

        return chunk;
    }
}
