package net.kjp12.plymouth.common;// Created Mar. 03, 2021 @ 10:36

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.world.level.ServerWorldProperties;

import java.util.UUID;

/**
 * Utility class for deriving the UUIDs from various objects.
 *
 * @author KJP12
 * @since 0.0.0
 **/
public final class UUIDHelper {
    public static final String
            PLYMOUTH = "Plymouth",
            DAMAGE_SOURCE = "Damage Source",
            ENTITY = "Entity",
            BLOCK = "Block",
            WORLD = "World";
    public static final long // Plymouth hash with forced UUID v2 to indicate NCP, a reserved type. This also distinguishes the UUID from actual player (v3 and v4) UUIDs.
            PLYMOUTH_HEADER = ((long) PLYMOUTH.hashCode() & 0xFFFF0FFFL) | 0x200L,
            DAMAGE_SOURCE_HEADER = ((long) DAMAGE_SOURCE.hashCode() << 32) | PLYMOUTH_HEADER,
            ENTITY_HEADER = ((long) ENTITY.hashCode() << 32) | PLYMOUTH_HEADER,
            BLOCK_HEADER = ((long) BLOCK.hashCode() << 32) | PLYMOUTH_HEADER,
            WORLD_HEADER = ((long) WORLD.hashCode() << 32) | PLYMOUTH_HEADER,
            LOW_BITS = 0x00000000_ffffffffL;

    public static final UUID
            ANONYMOUS_UUID = Util.NIL_UUID,
            ANONYMOUS_WORLD_UUID = new UUID(WORLD_HEADER, 0L),
            ANONYMOUS_ENTITY_UUID = new UUID(ENTITY_HEADER, 0L),
            ANONYMOUS_BLOCK_UUID = new UUID(BLOCK_HEADER, 0L);

    /**
     * Utility method for getting the attacking entity from the damage source.
     *
     * @param source The damage source to get the attacker from.
     * @return attacker, source entity or null.
     */
    public static Entity getEntity(DamageSource source) {
        var attacker = source.getAttacker();
        return attacker != null ? attacker : source.getSource();
    }

    /**
     * @param world The world to derive the UUID of.
     * @return The UUID of the world. May be {@link #ANONYMOUS_WORLD_UUID} if input is null.
     */
    public static UUID getUUID(ServerWorld world) {
        if (world == null) {
            return ANONYMOUS_WORLD_UUID;
        } else {
            var id = world.getRegistryKey().getValue();
            return new UUID(WORLD_HEADER, (long) ((ServerWorldProperties) world.getLevelProperties()).getLevelName().hashCode() << 32L | ((long) (id.getNamespace().hashCode() ^ id.getPath().hashCode()) & LOW_BITS));
        }
    }

    /**
     * @param damage The damage source to derive the UUID of.
     * @return The UUID of the attacker, source, or of the raw damage itself.
     */
    public static UUID getUUID(DamageSource damage) {
        var source = getEntity(damage);
        return source != null ? getUUID(source) : new UUID(ENTITY_HEADER, PLYMOUTH_HEADER | (long) damage.getName().hashCode() & LOW_BITS);
    }

    /**
     * @param entity The entity to derive the UUID of.
     * @return entity UUID if player, else the derived UUID if regular entity, else {@link #ANONYMOUS_ENTITY_UUID} if null.
     */
    public static UUID getUUID(Entity entity) {
        if (entity == null) {
            return ANONYMOUS_ENTITY_UUID;
        } else if (entity instanceof PlayerEntity) {
            return entity.getUuid();
        } else {
            var id = Registries.ENTITY_TYPE.getId(entity.getType());
            return new UUID(ENTITY_HEADER, (long) id.getNamespace().hashCode() << 32L | ((long) id.getPath().hashCode() & LOW_BITS));
        }
    }

    /**
     * @param block The block to derive the UUID of.
     * @return The UUID of the block. May be {@link #ANONYMOUS_BLOCK_UUID} if input is null.
     */
    public static UUID getUUID(Block block) {
        if (block == null) {
            return ANONYMOUS_BLOCK_UUID;
        } else {
            var id = Registries.BLOCK.getId(block);
            return new UUID(BLOCK_HEADER, (long) id.getNamespace().hashCode() << 32L | ((long) id.getPath().hashCode() & LOW_BITS));
        }
    }

    /**
     * @param uuid The UUID to test.
     * @return true if most significant bits matches {@link #DAMAGE_SOURCE_HEADER}.
     */
    public static boolean isDamageSource(UUID uuid) {
        return uuid.getMostSignificantBits() == DAMAGE_SOURCE_HEADER;
    }

    /**
     * @param uuid The UUID to test.
     * @return true if most significant bits matches {@link #ENTITY_HEADER}.
     */
    public static boolean isEntity(UUID uuid) {
        return uuid.getMostSignificantBits() == ENTITY_HEADER;
    }

    /**
     * @param uuid The UUID to test.
     * @return true if null or if most significant bits matches {@link #BLOCK_HEADER}.
     */
    public static boolean isBlock(UUID uuid) {
        return uuid == null || uuid.getMostSignificantBits() == BLOCK_HEADER;
    }

    /**
     * @param uuid The UUID to test.
     * @return true if most significant bits matches {@link #WORLD_HEADER}.
     */
    public static boolean isWorld(UUID uuid) {
        return uuid.getMostSignificantBits() == WORLD_HEADER;
    }
}
