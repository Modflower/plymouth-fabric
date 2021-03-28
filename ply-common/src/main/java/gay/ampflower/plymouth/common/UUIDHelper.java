package gay.ampflower.plymouth.common;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

/**
 * @author Ampflower
 * @since 0.0.0
 **/
public final class UUIDHelper {
    private static final long // 0x809ee372 = "Helium".hashCode() // Bootstrap Optimization
            HELIUM_HEADER = 0x809ee372_00000000L,
            ENTITY_HEADER = 0x809ee372_7c02d003L, // "Entity".hashCode()
            BLOCK_HEADER = 0x809ee372_03d4d46dL, // "Block".hashCode()
            WORLD_HEADER = 0x809ee372_04fe2b72L; // "World".hashCode()

    public static final UUID
            ANONYMOUS_UUID = Util.NIL_UUID,
            ANONYMOUS_ENTITY_UUID = new UUID(ENTITY_HEADER, 0L),
            ANONYMOUS_BLOCK_UUID = new UUID(BLOCK_HEADER, 0L);

    public static UUID getUUID(DamageSource damage) {
        var attacker = damage.getAttacker();
        if (attacker != null) {
            return getUUID(attacker);
        }
        var source = damage.getSource();
        if (source != null) {
            return getUUID(source);
        }
        return new UUID(ENTITY_HEADER, HELIUM_HEADER | (long) damage.getName().hashCode() & 0xFFFF_FFFFL);
    }

    public static UUID getUUID(Entity entity) {
        if (entity == null) {
            return ANONYMOUS_ENTITY_UUID;
        } else if (entity instanceof PlayerEntity) {
            return entity.getUuid();
        } else {
            var id = Registry.ENTITY_TYPE.getId(entity.getType());
            return new UUID(ENTITY_HEADER, (long) id.getNamespace().hashCode() << 32L | ((long) id.getPath().hashCode() & 0xFFFF_FFFFL));
        }
    }

    public static UUID getUUID(Block block) {
        if (block == null) {
            return ANONYMOUS_BLOCK_UUID;
        } else {
            var id = Registry.BLOCK.getId(block);
            return new UUID(BLOCK_HEADER, (long) id.getNamespace().hashCode() << 32L | ((long) id.getPath().hashCode() & 0xFFFF_FFFFL));
        }
    }
}
