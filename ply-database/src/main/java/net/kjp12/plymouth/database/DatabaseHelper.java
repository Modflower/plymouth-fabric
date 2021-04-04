package net.kjp12.plymouth.database;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.level.ServerWorldProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class DatabaseHelper implements ModInitializer {
    public static final Path config = Path.of(".", "config");
    public static final Logger LOGGER = LogManager.getLogger("Plymouth Tracker");

    public static Plymouth database;

    public static final Set<Property<?>> bannedProperties = Set.of(Properties.WATERLOGGED);

    /**
     * Anonymous Header Hashes, used for generating a UUID for blocks and entities
     * to interact with locked blocks, and to log in the database with the type intact.
     * <p>
     * If a new block or entity does try to modify a block, it will have full access
     * due to no permission checks existing for the new code. Beware of this.
     */
    private static final int
            HASHED_ENTITY = 0x809ee372 ^ 0x7c02d003,
            HASHED_BLOCK = 0x809ee372 ^ 0x03d4d46d,
            HASHED_WORLD = 0x809ee372 ^ 0x04fe2b72;

    static {
        try {
            var properties = new java.util.Properties();
            var props = config.resolve("plymouth.db.properties");
            if (Files.notExists(props)) {
                var legacyOptions = config.resolve("helium.db.properties");
                if (Files.exists(legacyOptions)) {
                    Files.move(legacyOptions, props);
                } else {
                    Files.createDirectories(config);
                    Files.createFile(props);
                    try (var os = Files.newOutputStream(props)) {
                        properties.put("helium$url", "jdbc:postgresql://127.0.0.1:5432/helium");
                        properties.put("user", "username");
                        properties.put("password", "password");
                        properties.put("closeOnError", "true");
                        properties.store(os, "Please fill out these properties to your needs. Supported JDBC drivers: PostgreSQL");
                    }
                    LOGGER.warn("Plymouth wasn't present, using NoOP.");
                    database = new PlymouthNoOP();
                }
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
    }

    public static <K, V> String toJson(Map<K, V> map, Set<K> banned, Function<K, String> kString, Function<V, String> vString) {
        var properties = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (banned.contains(k)) return;
            properties.append('"').append(kString.apply(k).replace("\"", "\\\"").replace("\\", "\\\\")).append("\":\"").append(vString.apply(v).replace("\"", "\\\"").replace("\\", "\\\\")).append("\",");
        });
        return (properties.length() == 1 ? properties.append('}') : properties.replace(properties.length() - 1, properties.length(), "}")).toString();
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
        // Legacy name.
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
        var id = world.getRegistryKey().getValue();
        return HASHED_WORLD ^ ((ServerWorldProperties) world.getLevelProperties()).getLevelName().hashCode() ^ id.getNamespace().hashCode() ^ id.getPath().hashCode();
    }

    @Override
    public void onInitialize() {
        // no-op, most of the init is in static.
    }
}
