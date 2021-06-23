package net.kjp12.plymouth.database;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.kjp12.plymouth.database.cache.*;
import net.kjp12.plymouth.database.records.*;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.level.ServerWorldProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.Driver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import static net.kjp12.plymouth.database.DatabaseHelper.*;

/**
 * Driver Adaptor for PostgreSQL. Due to how many features that Postgres has that standard SQL does not,
 * this adaptor is made to take advantage of those features.
 *
 * @author kjp12
 * @since 0.0.0
 */
@Query(query = "cause_id=?",
        values = "causeUuid",
        mask = LookupRecord.FLAG_BY)
@Query(query = "cause_pos=(?,?,?,?)::ipos",
        values = {"minPos.getX()", "minPos.getY()", "minPos.getZ()", "<0?^.worldIndex(causeWorld)>0"},
        mask = LookupRecord.FLAG_AT | LookupRecord.FLAG_AREA, maskRq = LookupRecord.FLAG_AT)
@Query(query = "cause_pos>=(?,?,?,?)::ipos and cause_pos<=(?,?,?,?)::ipos",
        values = {"minPos.getX()", "minPos.getY()", "minPos.getZ()", "<0?^.worldIndex(causeWorld)>0", "maxPos.getX()", "maxPos.getY()", "maxPos.getZ()", "<0"},
        mask = LookupRecord.FLAG_AT | LookupRecord.FLAG_AREA, maskRq = LookupRecord.FLAG_AREA)
public class PlymouthPostgres extends PlymouthSQL implements Plymouth {
    private static final Logger log = LogManager.getLogger(PlymouthPostgres.class);
    // We don't need reverse lookup, this is perfectly acceptable.
    // In case we do need reverse lookup, we can batch as needed.
    private final Set<UUID>
            players = new HashSet<>(32);
    private final Int2IntMap
            blocks = new Int2IntOpenHashMap(32),
            worlds = new Int2IntOpenHashMap(32);

    public PlymouthPostgres() throws NoClassDefFoundError {
        super(new Driver());
    }

    public PlymouthPostgres(String uri, Properties properties) throws PlymouthException, NoClassDefFoundError {
        super(new Driver(), uri, properties);
    }

    {
        try {
            blockLookupCache = new StatementCache<>(this, BlockLookupRecord.class, PlymouthPostgres.class.getMethod("blockRecordFromLookup", int.class, int.class, int.class, String.class, UUID.class, UUID.class, int.class, int.class, int.class, String.class, String.class, String.class, Timestamp.class, boolean.class));
            deathLookupCache = new StatementCache<>(this, DeathLookupRecord.class, PlymouthPostgres.class.getMethod("deathRecordFromLookup", int.class, int.class, int.class, String.class, UUID.class, UUID.class, double.class, double.class, double.class, String.class, UUID.class, UUID.class, Timestamp.class, boolean.class));
            inventoryLookupCache = new StatementCache<>(this, InventoryLookupRecord.class, PlymouthPostgres.class.getMethod("inventoryRecordFromLookup", int.class, int.class, int.class, String.class, UUID.class, UUID.class, int.class, int.class, int.class, String.class, UUID.class, UUID.class, Timestamp.class, boolean.class, String.class, InputStream.class, int.class));
        } catch (ReflectiveOperationException roe) {
            throw new PlymouthException(roe, "Failed to initialise lookup caches.", blockLookupCache, deathLookupCache, inventoryLookupCache);
        }
    }

    /**
     * Initializes the database if it hasn't been initialized before. Since this is dedicated to PostgreSQL,
     * Postgres-specific features has been taken advantage, such as inheritance.
     *
     * <h3><code>baction</code> - Block Action</h3>
     * The action taken in the world.
     * <ul>
     *     <li><b>BREAK</b> - They broke the block.</li>
     *     <li><b>PLACE</b> - They placed the block.</li>
     *     <li><b>USE</b> - They used the block.</li>
     * </ul>
     *
     * <h3><code>dimension</code></h3>
     * The dimension that the action would have been taken in.
     * Preloaded with <code>minecraft:overworld</code>, <code>minecraft:the_nether</code> and <code>minecraft:the_end</code>.
     * Any other dimensions may be added at runtime, as it is possible for dimensions to be dynamically generated through
     * datapacks and mods.
     *
     * <h3><code>ipos</code> - Integer Position</h3>
     * The position represented in integer X, Y, Z coordinates along with the originating dimension.
     *
     * <h3><code>dpos</code> - Double-precision Position</h3>
     * The position represented in double X, Y, Z coordinates along with the originating dimension.
     */
    public void initializeDatabase() throws PlymouthException {
        Statement statement = null;
        try {
            getElseInsertUser = connection.prepareStatement("SELECT get_else_insert_user(?, ?);");
            getElseInsertWorld = connection.prepareStatement("SELECT get_else_insert_world(?, ?);");
            getElseInsertBlock = connection.prepareStatement("SELECT get_else_insert_block(?, ?::jsonb);");
            insertBlocks = connection.prepareStatement("INSERT INTO blocks (cause_id, cause_raw, target_pos, block, action, nbt, time) VALUES (?, ?, (?, ?, ?, ?)::ipos, ?, ?::block_action, ?, ?);");
            insertDeaths = connection.prepareStatement("INSERT INTO deaths (cause_id, cause_raw, target_id, target_raw, target_pos, time) VALUES (?, ?, ?, ?, (?, ?, ?, ?)::dpos, ?);");
            insertItems = connection.prepareStatement("INSERT INTO items (cause_id, cause_raw, target_id, target_raw, target_pos, item, nbt, delta, time) VALUES (?, ?, ?, ?, (?, ?, ?, ?)::ipos, ?, ?, ?, ?);");
            getUsername = connection.prepareStatement("SELECT name FROM users_table WHERE index = ?;");
            // getBlocksInAreaDuring =   connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name, cause_id, cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, (target_pos).x, (target_pos).y, (target_pos).z, action, bt.name, bt.properties FROM blocks LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN blocks_table bt ON (bt.index = block) WHERE target_pos >= (?, ?, ?, ?)::ipos AND target_pos <= (?, ?, ?, ?)::ipos                  AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");
            // getBlocksInAreaDuringBy = connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name,           cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, (target_pos).x, (target_pos).y, (target_pos).z, action, bt.name, bt.properties FROM blocks LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN blocks_table bt ON (bt.index = block) WHERE target_pos >= (?, ?, ?, ?)::ipos AND target_pos <= (?, ?, ?, ?)::ipos AND cause_id = ? AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");

            // getDeathsInAreaDuring =   connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name, cause_id, cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, tt.name, target_id, target_raw, (target_pos).x, (target_pos).y, (target_pos).z FROM deaths LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN users_table tt ON (tt.index = target_id) WHERE target_pos >= (?, ?, ?, ?)::dpos AND target_pos <= (?, ?, ?, ?)::dpos                  AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");
            // getDeathsInAreaDuringBy = connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name,           cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, tt.name, target_id, target_raw, (target_pos).x, (target_pos).y, (target_pos).z FROM deaths LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN users_table tt ON (tt.index = target_id) WHERE target_pos >= (?, ?, ?, ?)::dpos AND target_pos <= (?, ?, ?, ?)::dpos AND cause_id = ? AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");

            // getInventoryInAreaDuring =   connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name, cause_id, cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, tt.name, target_id, target_raw, (target_pos).x, (target_pos).y, (target_pos).z, item, nbt, delta FROM items LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN users_table tt ON (tt.index = target_id) WHERE target_pos >= (?, ?, ?, ?)::ipos AND target_pos <= (?, ?, ?, ?)::ipos                  AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");
            // getInventoryInAreaDuringBy = connection.prepareStatement("SELECT time AT TIME ZONE 'UTC', undone, ct.name,           cause_raw, (cause_pos).x, (cause_pos).y, (cause_pos).z, tt.name, target_id, target_raw, (target_pos).x, (target_pos).y, (target_pos).z, item, nbt, delta FROM items LEFT OUTER JOIN users_table ct ON (ct.index = cause_id) LEFT OUTER JOIN users_table tt ON (tt.index = target_id) WHERE target_pos >= (?, ?, ?, ?)::ipos AND target_pos <= (?, ?, ?, ?)::ipos AND cause_id = ? AND time > ? AND time < ? ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET ?;");
            try {
                //noinspection unused - We don't have any schema bumps that'd require a change at the moment.
                var results = connection.prepareStatement("SELECT schema FROM plymouth_metadata;").executeQuery();
                // switch (results.getInt(1)) {
                //     // We won't have cases for now.
                // }
                return;
            } catch (SQLException ignore) {
                // We'll fall through to allow for backwards compatibility.
            }
            try {
                connection.prepareStatement("SELECT now_utc();").executeQuery();
                log.info("Starting migration to schema 1.");
                // We'll have to do the mass migration here. We're going to be upgrading from schema 0 to schema 1
                connection.setAutoCommit(false);
                // This allows rollback of changes in case something goes horribly wrong, that is, if horribly wrong is resettable.
                Savepoint start = connection.setSavepoint();
                try {
                    // Implementation detail: Within the massive transaction,
                    // we're doing semi-atomic operations to allow for fail-fast when it does occur,
                    // and to allow narrowing down which statement actually caused the issue.

                    log.info("Creating metadata and setting schema to 1.");
                    statement = connection.createStatement();
                    // Create the metadata table then insert `1` into it.
                    statement.addBatch("CREATE TABLE plymouth_metadata(schema INT NOT NULL);");
                    statement.addBatch("INSERT INTO plymouth_metadata (schema) VALUES (1);");
                    statement.executeBatch();

                    // Note that for the following two migrations,
                    // we can safely operate solely off of the mutation table as blocks and deaths delegate off the table,
                    // inserting into it as they get inserted into.

                    // Mutate cause_id to be a UUID based off of users_table.
                    // We're migrating more manually as Postgres doesn't allow for off-table references as far as I can tell.
                    log.info("Migrating mutation.cause_id to use UUID over serial ID. users_table will remain intact for name saving.");
                    statement.addBatch("ALTER TABLE mutation ADD cause_uid uuid REFERENCES users_table (uid);");
                    statement.addBatch("UPDATE mutation SET cause_uid = (SELECT uid FROM users_table WHERE index = cause_id);");
                    statement.addBatch("ALTER TABLE mutation DROP cause_id;");
                    statement.addBatch("ALTER TABLE mutation RENAME cause_uid TO cause_id;");
                    // We're enforcing the old semantics of NOT NULL. We couldn't set it at creation time as the entire column would've been null.
                    statement.addBatch("ALTER TABLE mutation ALTER COLUMN cause_id SET NOT NULL;");
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    // Mutate cause_raw to be a UUID based off of entities_table.
                    log.info("Migrating mutation.cause_raw to use UUID over serial ID, allowing removal of entities_table.");
                    statement.addBatch("ALTER TABLE mutation ADD cause_uid uuid;");
                    statement.addBatch("UPDATE mutation SET cause_uid = (SELECT uid FROM entities_table WHERE index = cause_raw);");
                    statement.addBatch("ALTER TABLE mutation DROP cause_raw;");
                    statement.addBatch("ALTER TABLE mutation RENAME cause_uid TO cause_raw;");
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    // blocks will be migrated from pos to target_pos to better standardise the tables.
                    log.info("Migrating blocks.pos to target_pos.");
                    statement.addBatch("ALTER TABLE blocks RENAME pos TO target_pos;");
                    statement.executeBatch();
                    log.info("Successfully migrated pos to target_pos.");

                    // deaths requires a bit more involvement as it has its own set of ID references.

                    // Mutate target_id to be UUID based off of users_table.
                    log.info("Migrating deaths.target_id to use UUID over serial ID.");
                    statement.addBatch("ALTER TABLE deaths ADD target_uid uuid REFERENCES users_table (uid);");
                    statement.addBatch("UPDATE deaths SET target_uid = (SELECT uid FROM users_table WHERE index = target_id);");
                    statement.addBatch("ALTER TABLE deaths DROP target_id;");
                    statement.addBatch("ALTER TABLE deaths RENAME target_uid TO target_id;");
                    // Again, enforcing old semantics of NOT NULL.
                    statement.addBatch("ALTER TABLE deaths ALTER COLUMN target_id SET NOT NULL;");
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    // Mutate target_raw to be UUID based off of entities_table.
                    log.info("Migrating deaths.target_raw to use UUID over serial ID.");
                    statement.addBatch("ALTER TABLE deaths ADD target_uid uuid;");
                    statement.addBatch("UPDATE deaths SET target_uid = (SELECT uid FROM entities_table WHERE index = target_raw);");
                    statement.addBatch("ALTER TABLE deaths DROP target_raw;");
                    statement.addBatch("ALTER TABLE deaths RENAME target_uid TO target_raw;");
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    log.info("Migrating deaths.death_pos to target_pos.");
                    statement.addBatch("ALTER TABLE deaths RENAME death_pos TO target_pos;");
                    statement.executeBatch();
                    log.info("Successfully migrated death_pos to target_pos.");

                    // Mutate inventory_id to be target_id and UUID based off of users_table.
                    log.info("Migrating items.inventory_id to target_id.");
                    statement.addBatch("ALTER TABLE items ADD target_id uuid REFERENCES users_table (uid);");
                    statement.addBatch("UPDATE items SET target_id = (SELECT uid FROM users_table WHERE index = inventory_id);");
                    statement.addBatch("ALTER TABLE items DROP inventory_id;");
                    // This didn't enforce the not null semantic before, so, there's no reason to set it here.
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    // Mutate inventory_raw to be target_raw and UUID based off of entities_table.
                    log.info("Migrating items.inventory_raw to target_raw.");
                    statement.addBatch("ALTER TABLE items ADD target_raw uuid;");
                    statement.addBatch("UPDATE items SET target_raw = (SELECT uid FROM entities_table WHERE index = inventory_raw);");
                    statement.addBatch("ALTER TABLE items DROP inventory_raw;");
                    log.info("Successfully migrated {} entries.", statement.executeBatch()[1]);

                    log.info("Migrating items.inventory_pos to target_pos.");
                    statement.addBatch("ALTER TABLE items RENAME COLUMN inventory_pos TO target_pos;");
                    statement.executeBatch();
                    log.info("Successfully migrated inventory_pos to target_pos.");

                    log.info("Replacing data and action with item, nbt and delta...");
                    statement.addBatch("ALTER TABLE items DROP COLUMN data;");
                    statement.addBatch("ALTER TABLE items DROP COLUMN action;");
                    statement.addBatch("DROP TYPE inventory_action;");
                    statement.addBatch("ALTER TABLE items ADD item text;");
                    statement.addBatch("ALTER TABLE items ADD nbt bytea;");
                    statement.addBatch("ALTER TABLE items ADD delta int;");
                    statement.executeBatch();
                    log.info("Successfully dropped data and action.");

                    // Drop the entities table.
                    log.info("Dropping entities table...");
                    statement.addBatch("DROP FUNCTION get_else_insert_entity;");
                    statement.addBatch("DROP TABLE entities_table;");
                    statement.executeBatch();
                    log.info("Successfully dropped entities table.");

                    // Migrate `uid` to `index` for more logical naming.
                    log.info("Migrating users_table.uid to index...");
                    // Dropping and redefining needs to be done so the function can still be used as normal.
                    statement.addBatch("DROP FUNCTION get_else_insert_user;");
                    statement.addBatch("ALTER TABLE users_table DROP index;");
                    statement.addBatch("ALTER TABLE users_table RENAME uid TO index;");
                    statement.addBatch("ALTER TABLE users_table ADD PRIMARY KEY (index);");
                    statement.addBatch("CREATE FUNCTION get_else_insert_user(bname text, buid uuid) RETURNS uuid AS $$ INSERT INTO users_table(index, name) VALUES(buid, bname) ON CONFLICT(index) DO UPDATE SET name = bname RETURNING index $$ LANGUAGE SQL;");
                    statement.executeBatch();
                    log.info("Successfully migrated uid to index.");

                    log.info("Adding NBT to blocks...");
                    statement.addBatch("ALTER TABLE blocks ADD nbt bytea;");
                    statement.executeBatch();
                    log.info("Successfully added NBT column.");

                    // Commit all the changes at once.
                    connection.commit();
                    // Set autocommit back to true since we don't need to worry about this failing and leaving the database in an unknown state.
                    connection.setAutoCommit(true);
                } catch (SQLException sql) {
                    var t = new PlymouthException(sql, statement);
                    try {
                        // Rollback the database to before the failure.
                        connection.rollback(start);
                        // Close the connection; failure will be evident in runtime either way.
                        connection.close();
                    } catch (SQLException sql2) {
                        t.addSuppressed(sql2);
                    }
                    // Bypassing the regular Plymouth catch as we're unable to continue with a database, and this requires intervention.
                    throw new ExceptionInInitializerError(t);
                }
                return;
            } catch (SQLException ignore) {
            }
            statement = connection.createStatement();
            statement.addBatch("CREATE TABLE IF NOT EXISTS plymouth_metadata(schema INT NOT NULL);");
            statement.addBatch("INSERT INTO plymouth_metadata (schema) VALUES (1);");
            statement.addBatch("CREATE FUNCTION now_utc() RETURNS timestamp AS $$ SELECT now() AT TIME ZONE 'utc' $$ LANGUAGE SQL;");
            statement.addBatch("CREATE TYPE inventory_action AS ENUM ('TAKE', 'PUT');");
            statement.addBatch("CREATE TYPE block_action AS ENUM ('BREAK', 'PLACE', 'USE');");
            statement.addBatch("CREATE TYPE ipos AS (x int, y int, z int, d int);");
            statement.addBatch("CREATE TYPE dpos AS (x double precision, y double precision, z double precision, d int);");
            /* Block, User and Item Indexes */
            // Note: Helium-UUIDs will be used here under uid. *This is intentional.*
            // They are accompanied with an identifier if they are a helium UUID.
            statement.addBatch("CREATE TABLE IF NOT EXISTS users_table (index uuid PRIMARY KEY, name TEXT NOT NULL);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS worlds_table (index SERIAL PRIMARY KEY, name TEXT NOT NULL, dimension TEXT NOT NULL);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS blocks_table (index SERIAL PRIMARY KEY, name TEXT NOT NULL, properties jsonb NULL);");
            // This is the only index that would benefit, the rest are implied by UNIQUE.
            statement.addBatch("CREATE INDEX IF NOT EXISTS blocks_index ON blocks_table (name);");
            /* Late-init methods due to the above tables needing to exist */
            statement.addBatch("CREATE FUNCTION get_else_insert_user(bname text, buid uuid) RETURNS uuid AS $$ INSERT INTO users_table(index, name) VALUES(buid, bname) ON CONFLICT(index) DO UPDATE SET name = bname RETURNING index $$ LANGUAGE SQL;");
            statement.addBatch("CREATE FUNCTION get_else_insert_world(bname text, bdim text) RETURNS int AS $$ WITH s AS (SELECT index FROM worlds_table WHERE name = bname AND dimension = bdim), i AS (INSERT INTO worlds_table (name, dimension) SELECT bname, bdim WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            statement.addBatch("CREATE FUNCTION get_else_insert_block(bname text, bprops jsonb) RETURNS int AS $$ WITH s AS (SELECT index FROM blocks_table WHERE name = bname AND (properties = bprops OR properties IS NULL)), i AS (INSERT INTO blocks_table (name, properties) SELECT bname, bprops WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            /* Mutation tables */
            statement.addBatch("CREATE TABLE IF NOT EXISTS mutation(cause_id uuid REFERENCES users_table (index) NOT NULL, cause_raw uuid, cause_pos ipos, time timestamp NOT NULL DEFAULT now_utc(), undone boolean NOT NULL DEFAULT false);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS blocks(target_pos ipos NOT NULL, block int REFERENCES blocks_table (index), action block_action, nbt bytea) INHERITS (mutation);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS deaths(target_id uuid REFERENCES users_table (index) NOT NULL, target_raw uuid, target_pos dpos NOT NULL) INHERITS (mutation);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS items(target_id uuid REFERENCES users_table (index), target_raw uuid, target_pos ipos, item TEXT, nbt bytea, delta int) INHERITS (mutation);");
            statement.executeBatch();
        } catch (SQLException sql) {
            throw new PlymouthException(sql, statement, getElseInsertUser, getElseInsertWorld, getElseInsertBlock, insertBlocks, insertDeaths, insertItems, getUsername);
        }
    }

    @Override
    protected void handleBlockRecord(BlockRecord br) throws SQLException {
        insertBlocks.setObject(1, getUserIndex(br.userName, br.userId));
        insertBlocks.setObject(2, br.entityId);
        addVec3i(insertBlocks, 3, br.targetPos);
        insertBlocks.setInt(6, getWorldIndex(br.targetWorld));
        insertBlocks.setInt(7, getBlockIndex(br.block));
        insertBlocks.setString(8, br.action.name());
        insertBlocks.setBinaryStream(9, br.mkNbtStream());
        insertBlocks.setTimestamp(10, Timestamp.from(br.time), UTC);
        insertBlocks.addBatch();
    }

    @Override
    protected void handleDeathRecord(DeathRecord dr) throws SQLException {
        insertDeaths.setObject(1, getUserIndex(dr.causeName, dr.causeUserId));
        insertDeaths.setObject(2, dr.causeEntityId);
        insertDeaths.setObject(3, getUserIndex(dr.targetName, dr.targetUserId));
        insertDeaths.setObject(4, dr.targetEntityId);
        addVec3d(insertDeaths, 5, dr.targetPos);
        insertDeaths.setInt(8, getWorldIndex(dr.targetWorld));
        insertDeaths.setTimestamp(9, Timestamp.from(dr.time), UTC);
        insertDeaths.addBatch();
    }

    @Override
    protected void handleInventoryRecord(InventoryRecord ir) throws SQLException {
        insertItems.setObject(1, getUserIndex(ir.causeName, ir.causeUserId));
        insertItems.setObject(2, ir.causeEntityId);
        insertItems.setObject(3, getUserIndex(ir.targetName, ir.targetUserId));
        insertItems.setObject(4, ir.targetEntityId);
        addVec3i(insertItems, 5, ir.targetPos);
        if (ir.targetWorld == null) {
            insertItems.setNull(8, Types.INTEGER);
        } else {
            insertItems.setInt(8, getWorldIndex(ir.targetWorld));
        }
        insertItems.setString(9, Registry.ITEM.getId(ir.item).toString());
        insertItems.setBinaryStream(10, ir.mkNbtStream());
        insertItems.setInt(11, ir.delta);
        insertItems.setTimestamp(12, Timestamp.from(ir.time), UTC);
        insertItems.addBatch();
    }

    protected int getBlockIndex(BlockState state) throws PlymouthException {
        boolean f0 = areStatesUnnecessary(state);
        return blocks.computeIfAbsent(f0 ? state == null ? 0 : state.getBlock().hashCode() : state.hashCode(), (int h) -> {
            try {
                getElseInsertBlock.setObject(1, state == null ? null : Registry.BLOCK.getId(state.getBlock()).toString());
                getElseInsertBlock.setObject(2, f0 ? null : toJson(state.getEntries(), bannedProperties, Property::getName, Objects::toString));
                var o = getElseInsertBlock.executeQuery();
                if (!o.next()) throw new SQLException("?!");
                return o.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(sql, getElseInsertBlock);
            }
        });
    }

    protected UUID getUserIndex(String name, UUID uuid) throws PlymouthException {
        if (uuid != null && players.add(uuid)) try {
            getElseInsertUser.setObject(1, name);
            getElseInsertUser.setObject(2, uuid);
            var i = getElseInsertUser.executeQuery();
            if (!i.next()) throw new SQLException("?!");
            return i.getObject(1, UUID.class);
        } catch (SQLException sql) {
            throw new PlymouthException(sql, getElseInsertUser);
        }
        return uuid;
    }

    protected int getWorldIndex(World world) throws PlymouthException {
        return worlds.computeIfAbsent(DatabaseHelper.getHash(world), $ -> {
            try {
                getElseInsertWorld.setObject(1, ((ServerWorldProperties) world.getLevelProperties()).getLevelName());
                getElseInsertWorld.setObject(2, world.getRegistryKey().getValue().toString());
                var i = getElseInsertWorld.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(sql, getElseInsertWorld);
            }
        });
    }

    public int worldIndex(World world) throws PlymouthException {
        return getWorldIndex(world);
    }

    @Override
    public String getPlayerName(UUID uuid) throws PlymouthException {
        databaseLock.lock();
        try {
            getUsername.setObject(1, uuid);
            var results = getUsername.executeQuery();
            if (results.next()) {
                // We got a result, return the result from the name column.
                return results.getString(1);
            } else {
                // There's nothing to return, so, return null.
                return null;
            }
        } catch (SQLException sql) {
            throw new PlymouthException(sql, getUsername);
        } finally {
            databaseLock.unlock();
        }
    }

    // unfortunately, the scope of this requires a fair amount of work
    @Table("blocks")
    @Table(table = 1, value = "users_table", match = @Match(primary = "cause_id", secondary = "index"))
    @Table(table = 2, value = "blocks_table", match = @Match(primary = "block", secondary = "index"))
    public static BlockRecord blockRecordFromLookup(
            @Value({"cause_pos", "x"}) int cx, @Value({"cause_pos", "y"}) int cy, @Value({"cause_pos", "z"}) int cz, @Value(table = 1, value = "name") String cn, @Value("cause_id") UUID cu, @Value("cause_raw") UUID ce,
            @Value({"target_pos", "x"}) int tx, @Value({"target_pos", "y"}) int ty, @Value({"target_pos", "z"}) int tz, @Deprecated @Value(value = "action") String ba, @Value(table = 2, value = "name") String bn, @Value(table = 2, value = "properties") String bp,
            @Value("time at time zone 'utc'") Timestamp time, @Value("undone") boolean u
    ) {
        var bl = Registry.BLOCK.get(Identifier.tryParse(bn));
        var bs = bl.getDefaultState();
        if (bp != null && !bp.isBlank()) {
            var mgr = bl.getStateManager();
            var obj = GSON.fromJson(bp, JsonObject.class);
            for (var e : obj.entrySet()) {
                var prop = mgr.getProperty(e.getKey());
                if (prop == null) continue;
                var opt = prop.parse(e.getValue().getAsString());
                if (opt.isEmpty()) continue;
                //noinspection rawtypes,unchecked
                bs = bs.with((Property) prop, (Comparable) opt.get());
            }
        }
        return new BlockRecord(time.toInstant(), u, null, new BlockPos(cx, cy, cz), cn, cu, ce, null, new BlockPos(tx, ty, tz), BlockAction.valueOf(ba), bs, null);
    }

    @Table("deaths")
    @Table(table = 1, value = "users_table", match = @Match(primary = "cause_id", secondary = "index"))
    @Table(table = 2, value = "users_table", match = @Match(primary = "target_id", secondary = "index"))
    public static DeathRecord deathRecordFromLookup(
            @Value({"cause_pos", "x"}) int cx, @Value({"cause_pos", "y"}) int cy, @Value({"cause_pos", "z"}) int cz, @Value(table = 1, value = "name") String cn, @Value("cause_id") UUID cu, @Value("cause_raw") UUID ce,
            @Value({"target_pos", "x"}) double tx, @Value({"target_pos", "y"}) double ty, @Value({"target_pos", "z"}) double tz, @Value(table = 2, value = "name") String tn, @Value("target_id") UUID tu, @Value("target_raw") UUID te,
            @Value("time at time zone 'utc'") Timestamp time, @Value("undone") boolean u
    ) {
        return new DeathRecord(time.toInstant(), u, null, new BlockPos(cx, cy, cz), cn, cu, ce, null, new Vec3d(tx, ty, tz), tn, tu, te);
    }

    @Table("items")
    @Table(table = 1, value = "users_table", match = @Match(primary = "cause_id", secondary = "index"))
    @Table(table = 2, value = "users_table", match = @Match(primary = "target_id", secondary = "index"))
    public static InventoryRecord inventoryRecordFromLookup(
            @Value({"cause_pos", "x"}) int cx, @Value({"cause_pos", "y"}) int cy, @Value({"cause_pos", "z"}) int cz, @Value(table = 1, value = "name") String cn, @Value("cause_id") UUID cu, @Value("cause_raw") UUID ce,
            @Value({"target_pos", "x"}) int tx, @Value({"target_pos", "y"}) int ty, @Value({"target_pos", "z"}) int tz, @Value(table = 2, value = "name") String tn, @Value("target_id") UUID tu, @Value("target_raw") UUID te,
            @Value("time at time zone 'utc'") Timestamp time, @Value("undone") boolean u, @Value("item") String i, @Value("nbt") InputStream in, @Value("delta") int id
    ) {
        var item = Registry.ITEM.get(Identifier.tryParse(i));
        var is = new ItemStack(item, 1);
        var nbt = nbt(in);
        is.setTag(nbt);
        return new InventoryRecord(
                null, new BlockPos(cx, cy, cz), cn, cu, ce,
                null, new BlockPos(tx, ty, tz), tn, tu, te,
                time.toInstant(), u, item, nbt, id, is, 0);
    }

    public static NbtCompound nbt(InputStream stream) {
        if (stream == null) return null;
        NbtCompound in = null;
        try (var s = stream; var dis = new DataInputStream(s)) {
            in = NbtIo.read(dis);
        } catch (IOException ioe) {
            log.error("Failed to read NBT from database.", ioe);
        }
        return in;
    }
}
