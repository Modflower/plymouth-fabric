package net.kjp12.plymouth.antixray;// Created 2022-20-01T20:54:29

import net.minecraft.world.chunk.Chunk;

/**
 * ChunkManager interface for more lazily getting loaded chunks when possible.
 *
 * @author Ampflower
 * @since ${version}
 **/
public interface LazyChunkManager {
    /**
     * Fetches the chunk in varying states of loaded.
     * <p>
     * This is primarily intended to work around Concurrent Chunk Management Engine
     * sending and loading chunks mid-tick in a way where it can stall the server until
     * either the player intervenes in the case of single player, or
     * {@link net.minecraft.server.dedicated.DedicatedServerWatchdog Watchdog} or the
     * sysadmin intervenes in the case of a dedicated server.
     *
     * @param chunkX X coordinate of chunk to fetch.
     * @param chunkZ Z coordinate of chunk to fetch.
     * @return Chunk of varying state of initialised.
     */
    Chunk plymouth$getChunkLazy(int chunkX, int chunkZ);
}
