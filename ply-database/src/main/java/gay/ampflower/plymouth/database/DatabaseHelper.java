package gay.ampflower.plymouth.database;

import com.google.gson.Gson;
import gay.ampflower.plymouth.common.UUIDHelper;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.level.ServerWorldProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * General utilities for the database, including where the config should be, the logger, various database variables, texts and more.
 *
 * @author Ampflower
 * @since ${version}
 */
public final class DatabaseHelper {
    public static final Path config = Path.of(".", "config");
    public static final Logger LOGGER = LogManager.getLogger("Plymouth: Database");
    public static final int PAGE_SIZE = 8;

    static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
    final static Gson GSON = new Gson();
    final static ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor();

    public static Plymouth database;

    public static final Text tookText = new TranslatableText("plymouth.tracker.action.took").formatted(Formatting.RED);

    public static final Set<Property<?>> bannedProperties = Set.of(Properties.WATERLOGGED, Properties.POWERED);

    /**
     * Anonymous Header Hashes, used for generating a UUID for blocks and entities
     * to interact with locked blocks, and to log in the database with the type intact.
     * <p>
     * If a new block or entity does try to modify a block, it will have full access
     * due to no permission checks existing for the new code. Beware of this.
     */
    private static final int
            HASHED_ENTITY = 0x809ee372 ^ 0x7c02d003,
            HASHED_WORLD = 0x809ee372 ^ 0x04fe2b72;

    static {
        boolean shouldError = false;
        b:
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
                        properties.put("url", "jdbc:postgresql://127.0.0.1:5432/plymouth");
                        properties.put("user", "username");
                        properties.put("password", "password");
                        properties.put("closeOnError", "true");
                        properties.store(os, "Please fill out these properties to your needs. Supported JDBC drivers: PostgreSQL");
                    }
                    LOGGER.warn("Plymouth wasn't present, using NoOP.");
                    database = new PlymouthNoOP();
                    break b;
                }
            }
            try (var is = Files.newInputStream(props)) {
                properties.load(is);
            }
            shouldError = Boolean.parseBoolean(properties.getProperty("closeOnError"));
            var url = Objects.requireNonNullElseGet(properties.getProperty("url"), () -> properties.getProperty("helium$url"));
            database = url.startsWith("jdbc:postgresql:") ? new PlymouthPostgres(url, properties) : new PlymouthNoOP();
            database.initializeDatabase();
        } catch (NoClassDefFoundError | PlymouthException | IOException ncdfe) {
            if (shouldError) throw new RuntimeException("Cannot load Plymouth Driver Wrapper, exiting…", ncdfe);
            assert false : ncdfe;
            LOGGER.error("Cannot load Plymouth Driver Wrapper, using NoOP.", ncdfe);
            database = new PlymouthNoOP();
        }
        Objects.requireNonNull(database);
        if (!(database instanceof PlymouthNoOP)) {
            SERVICE.scheduleAtFixedRate(database::sendBatches, 1000, 100, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * @param state The block to test if storing the states of are unnecessary.
     * @return true if null, fire, or if all properties are banned.
     */
    public static boolean areStatesUnnecessary(BlockState state) {
        if (state == null || state.getBlock() instanceof AbstractFireBlock) return true;
        var entries = state.getEntries();
        if (entries.isEmpty()) return true;
        return bannedProperties.containsAll(entries.keySet());
    }

    /**
     * Primitive JSON object function to quickly write a map to a JSON. Does the absolute bare minimum.
     *
     * @param <K>     Key-type
     * @param <V>     Value-type
     * @param map     The map to convert into a JSON object.
     * @param banned  A set of keys that should not be added.
     * @param kString toString function for the key.
     * @param vString toString function for the value.
     * @return JSON object as a string.
     */
    public static <K, V> String toJson(Map<K, V> map, Set<K> banned, Function<K, String> kString, Function<V, String> vString) {
        var properties = new StringBuilder("{");
        for (var e : map.entrySet()) {
            var k = e.getKey();
            if (!banned.contains(k))
                properties.append('"').append(kString.apply(k).replace("\\", "\\\\").replace("\"", "\\\"")).append("\":\"")
                        .append(vString.apply(e.getValue()).replace("\\", "\\\\").replace("\"", "\\\"")).append("\",");
        }
        if (properties.length() == 1) {
            return properties.append('}').toString();
        } else {
            properties.setCharAt(properties.length() - 1, '}');
            return properties.toString();
        }
    }

    public static String getName(ServerWorld world) {
        if (world == null) {
            return "plymouth:unknown-world";
        } else {
            return ((ServerWorldProperties) world.getLevelProperties()).getLevelName() + ':' + world.getRegistryKey().getValue();
        }
    }

    public static String getName(DamageSource damage) {
        var source = UUIDHelper.getEntity(damage);
        return source != null ? getName(source) : "plymouth:" + damage.getName();
    }

    public static String getName(Entity entity) {
        if (entity == null) {
            return "plymouth:anonymous_entity";
        } else if (entity instanceof PlayerEntity) {
            return entity.getName().asString();
        } else {
            return Registry.ENTITY_TYPE.getId(entity.getType()).toString();
        }
    }

    public static int getHash(ServerWorld world) {
        if (world == null) return HASHED_WORLD;
        var id = world.getRegistryKey().getValue();
        return HASHED_WORLD ^ ((ServerWorldProperties) world.getLevelProperties()).getLevelName().hashCode() ^ id.getNamespace().hashCode() ^ id.getPath().hashCode();
    }

    private DatabaseHelper() {
    }

    public static void init() {
        // no-op, most of the init is in static.
    }

    public static void log(ResultSet resultSet, int index) throws SQLException {
        var metadata = resultSet.getMetaData();
        LOGGER.info("{} @ {} -> {} {}: {}", resultSet.getStatement(), index, metadata.getColumnName(index), metadata.getColumnTypeName(index), resultSet.getObject(index));
    }

    /**
     * Adds a Vec3i to a prepared statement offset by i.
     *
     * @param statement The statement to add the 3 integers to.
     * @param offset    The offset to add the Vec3i to.
     * @param pos       The Vec3i to add to the statement.
     * @return offset + 3
     */
    public static int addVec3i(PreparedStatement statement, int offset, Vec3i pos) throws SQLException {
        if (pos == null) pos = Vec3i.ZERO;
        statement.setInt(offset++, pos.getX());
        statement.setInt(offset++, pos.getY());
        statement.setInt(offset++, pos.getZ());
        return offset;
    }

    /**
     * Adds a Vec3d to a prepared statement offset by i.
     *
     * @param statement The statement to add the 3 doubles to.
     * @param offset    The offset to add the Vec3d to.
     * @param pos       The Vec3d to add to the statement.
     * @return offset + 3
     */
    public static int addVec3d(PreparedStatement statement, int offset, Vec3d pos) throws SQLException {
        if (pos == null) pos = Vec3d.ZERO;
        statement.setDouble(offset++, pos.x);
        statement.setDouble(offset++, pos.y);
        statement.setDouble(offset++, pos.z);
        return offset;
    }

    /**
     * Formats the input time to ISO Local Date with a hover card showing the full date in RFC 1123 format.
     *
     * @param instant The time to format.
     * @return The formatted time as a literal text.
     */
    public static Text timeToText(Instant instant) {
        var time = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        return new LiteralText(DateTimeFormatter.ISO_LOCAL_DATE.format(time)).styled(s -> s.withFormatting(Formatting.GRAY).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(DateTimeFormatter.RFC_1123_DATE_TIME.format(time)))));
    }

    private static <T extends Comparable<T>> Text mkPropertyText(BlockState state, Property<T> property) {
        return new LiteralText(property.getName()).formatted(Formatting.BLUE).append(": ").append(new LiteralText(property.name(state.get(property))).formatted(Formatting.GRAY)).append("\n");
    }

    /**
     * Appends the properties of the block onto the input mutable text.
     *
     * @param state The block state to fetch the properties of.
     * @param input The mutable text to append properties to.
     * @return input mutated with all the properties appended.
     */
    private static MutableText mkBlockStateHoverCard(BlockState state, MutableText input) {
        var props = state.getProperties();
        if (!props.isEmpty()) {
            input.append("\n");
            for (var prop : props) {
                input.append(mkPropertyText(state, prop));
            }
        }
        return input;
    }

    /**
     * Returns a text derived from the given block, including a hover card of the properties mimicking the in-inventory
     * hover card.
     *
     * @param state The block state to format into a text object.
     * @return The translatable text based off of the input.
     */
    public static MutableText fromBlockToText(BlockState state) {
        var block = state.getBlock();
        var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, mkBlockStateHoverCard(state, new TranslatableText(block.getTranslationKey())).append("\n").append(new LiteralText(Registry.BLOCK.getId(block).toString()).formatted(Formatting.DARK_GRAY)));
        return new TranslatableText(block.getTranslationKey()).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.LIGHT_PURPLE));
    }

    /**
     * Returns a text derived from the given stack, including a hover card of the item.
     *
     * @param stack The item stack to format into a text object.
     * @return The literal text based off of the input.
     */
    public static MutableText fromItemToText(ItemStack stack) {
        var text = new LiteralText("").append(stack.getName());
        var style = text.getStyle();
        if (stack.hasCustomName()) style = style.withFormatting(Formatting.ITALIC);
        var fmt = stack.getRarity().formatting;
        style = style.withFormatting(fmt == Formatting.WHITE ? Formatting.LIGHT_PURPLE : fmt);
        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack)));
        return text.setStyle(style);
    }

    public static MutableText fromPlayerToText(String userName, UUID userId, @Nullable UUID entityId) {
        return fromPlayerToText(null, null, userName, userId, entityId);
    }

    /**
     * Returns a text derived from the given username, user UUID and entity UUID.
     *
     * @param world    The world to name if the user ID is a world.
     * @param pos      The position to name if the user ID is a block.
     * @param userName The username of the user. Can also be an identifier.
     * @param userId   The UUID of the user. Maybe an NPC-based Plymouth identifier of an entity, block or world.
     * @param entityId The raw UUID of the entity should it not be a player.
     * @return Either the literal or translatable text based off of the input.
     */
    public static MutableText fromPlayerToText(ServerWorld world, BlockPos pos, String userName, UUID userId, @Nullable UUID entityId) {
        if (UUIDHelper.isEntity(userId)) {
            // Do an entity lookup using userName as an identifier.
            var optional = EntityType.get(userName);
            if (optional.isPresent()) {
                // We have a valid entity, make it based off of the SHOW_ENTITY hover event action.
                var type = optional.get();
                var onHover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(type, Objects.requireNonNullElse(entityId, userId), new TranslatableText(type.getTranslationKey())));
                return new TranslatableText(type.getTranslationKey()).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.BLUE));
            } else {
                // We don't havea  valid entity, we'll have to resort to manually generating the string.
                var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(userName + '\n' + Objects.requireNonNullElse(entityId, userId)));
                var i = userName.indexOf(':');
                return new LiteralText(i == -1 ? userName : userName.substring(i + 1)).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.DARK_BLUE));
            }
        } else if (UUIDHelper.isDamageSource(userId) || UUIDHelper.isWorld(userId) /*The world is an undefined format currently. For now, it shall fall through. TODO: implement world schema*/) {
            // We really have nothing we can do to lookup here,
            // nor should we really need to as the damage source is saved as helium:<src>.
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(userName + '\n' + userId));
            var i = userName.indexOf(':');
            return new LiteralText(i == -1 ? userName : userName.substring(i + 1)).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.DARK_AQUA));
        } else if (UUIDHelper.isBlock(userId)) {
            // Do a block lookup using userName as an identifier.
            var optional = Registry.BLOCK.getOrEmpty(Identifier.tryParse(userName));
            if (optional.isPresent()) {
                // We have a valid block, we can make use of the translation key.
                var block = optional.get();
                var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText(block.getTranslationKey()).append("\n" + userId));
                return new TranslatableText(block.getTranslationKey()).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.YELLOW));
            } else {
                // We don't have a valid block. Perhaps it was removed? Use the non-translatable version of the return to make a reasonable string.
                var onHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(userName + '\n' + userId));
                var i = userName.indexOf(':');
                return new LiteralText(i == -1 ? userName : userName.substring(i + 1)).styled(s -> s.withHoverEvent(onHover).withFormatting(Formatting.GOLD));
            }
        } else {
            // We're a player, we'll just present the output as one. Note: This may produce screwy output with legacy database schemas.
            var onHover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(EntityType.PLAYER, userId, new LiteralText(userName /*no name lookup required*/)));
            return new LiteralText(userName).styled(s -> s.withHoverEvent(onHover));
        }
    }
}
