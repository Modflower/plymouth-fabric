package net.kjp12.helium;

import net.kjp12.helium.helpers.IShadowBlockEntity;
import net.kjp12.helium.mixins.AccessorBlockTag;
import net.kjp12.plymouth.Plymouth;
import net.kjp12.plymouth.PlymouthException;
import net.kjp12.plymouth.PlymouthNoOP;
import net.kjp12.plymouth.PlymouthPostgres;
import net.kjp12.plymouth.mixins.AccessorServerWorld;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * This class is a late-init-type class. It's expecting that
 * by the time that it's called, all blocks would have been
 * registered.
 */
public class Helium {
    public static final Logger LOGGER = LogManager.getLogger("Helium");
    public static final UUID
            ANONYMOUS_UUID = Util.NIL_UUID,
            ANONYMOUS_ENTITY_UUID,
            ANONYMOUS_BLOCK_UUID;
    public static final Tag<Block>
            HIDDEN_BLOCKS,
            NO_SMEAR_BLOCKS;
    private static final long // 0x809ee372 = "Helium".hashCode() // Bootstrap Optimization
            ENTITY_HEADER = 0x809ee372_7c02d003L, // "Entity".hashCode()
            BLOCK_HEADER = 0x809ee372_03d4d46dL, // "Block".hashCode()
            WORLD_HEADER = 0x809ee372_04fe2b72L; // "World".hashCode()
    public static Plymouth database;
    /**
     * Anonymous Header Hashes, used for generating a UUID for blocks and entities
     * to interact with locked blocks, and to log in the database with the type intact.
     * <p>
     * If a new block or entity does try to modify a block, it will have full access
     * due to no permission checks existing for the new code. Beware of this.
     */
    private static int
            HASHED_ENTITY = 0x809ee372 ^ 0x7c02d003,
            HASHED_BLOCK = 0x809ee372 ^ 0x03d4d46d,
            HASHED_WORLD = 0x809ee372 ^ 0x04fe2b72;

    static {
        try {
            var properties = new Properties();
            var conf = Path.of(".", "config");
            var props = conf.resolve("helium.db.properties");
            if (Files.notExists(props)) {
                Files.createDirectories(conf);
                Files.createFile(props);
                try (var os = Files.newOutputStream(props)) {
                    properties.put("helium$url", "jdbc:postgresql://127.0.0.1:5432/helium");
                    properties.put("user", "username");
                    properties.put("password", "password");
                    properties.store(os, "Please fill out these properties to your needs. Supported drivers: PostgreSQL");
                }
                LOGGER.warn("Plymouth wasn't present, using NoOP.");
                database = new PlymouthNoOP();
            } else {
                try (var is = Files.newInputStream(props)) {
                    properties.load(is);
                }
                var url = properties.getProperty("helium$url");
                database = url.startsWith("jdbc:postgresql:") ? new PlymouthPostgres(url, properties) : new PlymouthNoOP();
                database.initializeDatabase();
            }
        } catch (NoClassDefFoundError | PlymouthException | IOException ncdfe) {
            assert false : ncdfe;
            LOGGER.error("Cannot load Plymouth Driver Wrapper, using NoOP.", ncdfe);
            database = new PlymouthNoOP();
        }
        //TODO: Replace with Fabric API when it becomes a thing.
        var accessor = AccessorBlockTag.getAccessor();
        HIDDEN_BLOCKS = accessor.get("helium:hidden");
        NO_SMEAR_BLOCKS = accessor.get("helium:no_smear");

        ANONYMOUS_ENTITY_UUID = new UUID(ENTITY_HEADER, 0L);
        ANONYMOUS_BLOCK_UUID = new UUID(BLOCK_HEADER, 0L);
    }

    public static String getName(DamageSource damage) {
        var attacker = damage.getAttacker();
        if (attacker != null) {
            return getName(attacker);
        }
        var source = damage.getSource();
        if (source != null) {
            return getName(source);
        }
        return "helium:" + damage.getName();
    }

    public static String getName(Entity entity) {
        if (entity == null) {
            return "helium:anonymous_entity";
        } else if (entity instanceof PlayerEntity) {
            return entity.getName().asString();
        } else {
            return Registry.ENTITY_TYPE.getId(entity.getType()).toString();
        }
    }

    public static int getHash(DamageSource damage) {
        var attacker = damage.getAttacker();
        if (attacker != null) {
            return getHash(attacker);
        }
        var source = damage.getSource();
        if (source != null) {
            return getHash(source);
        }
        return HASHED_ENTITY ^ 0x809ee372 ^ damage.getName().hashCode();
    }

    public static int getHash(Entity entity) {
        if (entity == null) {
            return HASHED_ENTITY;
        } else if (entity instanceof PlayerEntity) {
            return entity.getUuid().hashCode();
        } else {
            var id = Registry.ENTITY_TYPE.getId(entity.getType());
            return HASHED_ENTITY ^ id.getNamespace().hashCode() ^ id.getPath().hashCode();
        }
    }

    public static int getHash(ServerWorld world) {
        var id = world.getDimensionRegistryKey().getValue();
        return HASHED_WORLD ^ ((AccessorServerWorld) world).getWorldProperties().getLevelName().hashCode() ^ id.getNamespace().hashCode() ^ id.getPath().hashCode();
    }

    public static UUID getUUID(DamageSource damage) {
        var attacker = damage.getAttacker();
        if (attacker != null) {
            return getUUID(attacker);
        }
        var source = damage.getSource();
        if (source != null) {
            return getUUID(source);
        }
        return new UUID(ENTITY_HEADER, 0x809ee372_00000000L | (long) damage.getName().hashCode() & 0xFFFF_FFFFL);
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

    /**
     * Protection check blocks. If the cause is a non-player entity,
     * it'll attempt to get the cause of player's UUID or use the
     * anonymous hashes.
     *
     * @return true if the entity can break the block.
     */
    public static boolean canBreak(IShadowBlockEntity block, Entity breaker) {
        return block == null || (breaker instanceof PlayerEntity ? block.helium$canBreakBlock((PlayerEntity) breaker) : block.helium$canBreakBlock(getUUID(breaker)));
    }

    // We want to effect both blocks if at all possible.
    // Note: CHEST and TRAPPED_CHEST are not compatible and should be treated as two separate entities.
    //  This can be seen in game by setting one to LEFT and the other to RIGHT and trying to open one of them.
    //  We may ignore this as long the other block we match against is of a fitting state.
    public static BlockPos getOtherPos(World world, BlockPos pos) {
        BlockPos otherPos = null;
        BlockState state = world.getBlockState(pos), otherState;
        var block = state.getBlock();
        if (block instanceof ChestBlock) {
            var type = state.get(ChestBlock.CHEST_TYPE);
            var face = state.get(ChestBlock.FACING);

            if ((otherPos = type == ChestType.SINGLE ? null : pos.offset(type == ChestType.LEFT ? face.rotateYClockwise() : face.rotateYCounterclockwise())) != null &&
                    ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                            otherState.get(ChestBlock.FACING) != face ||
                            otherState.get(ChestBlock.CHEST_TYPE) != type.getOpposite())) {
                otherPos = null;
            }
        } else if (block instanceof BedBlock) {
            var half = state.get(BedBlock.PART);
            var face = state.get(BedBlock.FACING);
            otherPos = pos.offset(half == BedPart.FOOT ? face : face.getOpposite());

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                    otherState.get(BedBlock.FACING) != face ||
                    otherState.get(BedBlock.PART) == half) {
                otherPos = null;
            }
        } else if (block instanceof DoorBlock) {
            var half = state.get(DoorBlock.HALF);
            otherPos = pos.offset(half == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block || otherState.get(DoorBlock.HALF) == half) {
                otherPos = null;
            }
        }
        return otherPos;
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
