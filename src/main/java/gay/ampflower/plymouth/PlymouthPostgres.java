package gay.ampflower.plymouth;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import gay.ampflower.helium.Helium;
import gay.ampflower.plymouth.mixins.AccessorServerWorld;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.Driver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import static gay.ampflower.plymouth.PlymouthHelper.toJson;

/**
 * Driver Adaptor for PostgreSQL. Due to how many features that Postgres has that standard SQL does not,
 * this adaptor is made to take advantage of those features.
 *
 * @author Ampflower
 * @since 0.0.0
 */
public class PlymouthPostgres extends PlymouthSQL implements Plymouth {
    private static final Logger log = LogManager.getLogger(PlymouthPostgres.class);
    // We don't need reverse lookup, this is perfectly acceptable.
    // In case we do need reverse lookup, we can batch as needed.
    private Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private Int2IntMap
            items = new Int2IntOpenHashMap(32),
            blocks = new Int2IntOpenHashMap(32),
            worlds = new Int2IntOpenHashMap(32),
            players = new Int2IntOpenHashMap(32),
            entities = new Int2IntOpenHashMap(32);
    private PreparedStatement
            insertBlocks, insertDeaths, insertItems,
            getElseInsertUser, getElseInsertBlock, getElseInsertEntity, getElseInsertWorld, getUsername,
            getModificationsInArea, getModificationsInAreaDuring, getModificationsInAreaBy, getModificationsInAreaDuringBy;
    private ReentrantLock databaseLock = new ReentrantLock();

    public PlymouthPostgres(String uri, Properties properties) throws PlymouthException, NoClassDefFoundError {
        super(new Driver(), uri, properties);
    }

    /**
     * Initializes the database if it hasn't been initialized before. Since this is dedicated to PostgreSQL,
     * Postgres-specific features has been taken advantage, such as inheritance.
     *
     * <h3><code>iaction</code> - Inventory Action</h3>
     * The action taken in the inventory.
     * <ul>
     *     <li><b>TAKE</b> - They took from the inventory.</li>
     *     <li><b>PUT</b> - They put into an inventory.</li>
     * </ul>
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
            getElseInsertEntity = connection.prepareStatement("SELECT get_else_insert_entity(?);");
            insertBlocks = connection.prepareStatement("INSERT INTO blocks (cause_id, cause_raw, pos, block, action) VALUES (?, ?, (?, ?, ?, ?)::ipos, ?, ?::block_action);");
            insertDeaths = connection.prepareStatement("INSERT INTO deaths (cause_id, cause_raw, target_id, target_raw, death_pos) VALUES (?, ?, ?, ?, (?, ?, ?, ?)::dpos);");
            insertItems = connection.prepareStatement("INSERT INTO items (cause_id, ?, inventory_id, ?, data, action) VALUES (?, ?, ?, ?, ?::jsonb, ?);");
            getUsername = connection.prepareStatement("SELECT name FROM users_table WHERE uid = ?;");
            getModificationsInArea = connection.prepareStatement("SELECT * FROM blocks WHERE (pos).x > ? AND (pos).x < ? AND (pos).y > ? AND (pos).y < ? AND (pos).z > ? AND (pos).z < ?;");
            getModificationsInAreaBy = connection.prepareStatement("SELECT * FROM blocks WHERE cause_id = ? AND (pos).x > ? AND (pos).x < ? AND (pos).y > ? AND (pos).y < ? AND (pos).z > ? AND (pos).z < ?;");
            getModificationsInAreaDuring = connection.prepareStatement("SELECT * FROM blocks WHERE time > ? AND time < ? AND (pos).x > ? AND (pos).x < ? AND (pos).y > ? AND (pos).y < ? AND (pos).z > ? AND (pos).z < ?;");
            getModificationsInAreaDuringBy = connection.prepareStatement("SELECT * FROM blocks WHERE time > ? AND time < ? AND cause_id = ? AND (pos).x > ? AND (pos).x < ? AND (pos).y > ? AND (pos).y < ? AND (pos).z > ? AND (pos).z < ?;");
            try {
                connection.prepareStatement("SELECT now_utc();").executeQuery();
                return;
            } catch (SQLException ignore) {
            }
            statement = connection.createStatement();
            statement.addBatch("CREATE FUNCTION now_utc() RETURNS timestamp AS $$ SELECT now() AT TIME ZONE 'utc' $$ LANGUAGE SQL;");
            statement.addBatch("CREATE TYPE inventory_action AS ENUM ('TAKE', 'PUT');");
            statement.addBatch("CREATE TYPE block_action AS ENUM ('BREAK', 'PLACE', 'USE');");
            statement.addBatch("CREATE TYPE ipos AS (x int, y int, z int, d int);");
            statement.addBatch("CREATE TYPE dpos AS (x double precision, y double precision, z double precision, d int);");
            /* Block, User and Item Indexes */
            // Note: Helium-UUIDs will be used here under uid. *This is intentional.*
            // They are accompanied with an identifier if they are a helium UUID.
            statement.addBatch("CREATE TABLE IF NOT EXISTS users_table (index SERIAL PRIMARY KEY, name TEXT NOT NULL, uid uuid NOT NULL UNIQUE);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS worlds_table (index SERIAL PRIMARY KEY, name TEXT NOT NULL, dimension TEXT NOT NULL);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS blocks_table (index SERIAL PRIMARY KEY, name TEXT NOT NULL, properties jsonb NULL);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS entities_table (index SERIAL PRIMARY KEY, uid uuid NOT NULL UNIQUE);");
            statement.addBatch("CREATE INDEX IF NOT EXISTS user_index ON users_table (index, uid);");
            statement.addBatch("CREATE INDEX IF NOT EXISTS block_index ON blocks_table (index);");
            statement.addBatch("CREATE INDEX IF NOT EXISTS entity_index ON entities_table (index);");
            /* Late-init methods due to the above tables needing to exist */
            statement.addBatch("CREATE FUNCTION get_else_insert_user(bname text, buid uuid) RETURNS int AS $$ WITH s AS (SELECT index FROM users_table WHERE uid = buid), i AS (INSERT INTO users_table (name, uid) SELECT bname, buid WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index), u AS (UPDATE users_table SET name = bname WHERE NOT EXISTS (SELECT 1 FROM i) AND uid = buid AND name != bname) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            statement.addBatch("CREATE FUNCTION get_else_insert_world(bname text, bdim text) RETURNS int AS $$ WITH s AS (SELECT index FROM worlds_table WHERE name = bname AND dimension = bdim), i AS (INSERT INTO worlds_table (name, dimension) SELECT bname, bdim WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            statement.addBatch("CREATE FUNCTION get_else_insert_block(bname text, bprops jsonb) RETURNS int AS $$ WITH s AS (SELECT index FROM blocks_table WHERE name = bname AND (properties = bprops OR properties IS NULL)), i AS (INSERT INTO blocks_table (name, properties) SELECT bname, bprops WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            statement.addBatch("CREATE FUNCTION get_else_insert_entity(buid uuid) RETURNS int AS $$ WITH s AS (SELECT index FROM entities_table WHERE uid = buid), i AS (INSERT INTO entities_table (uid) SELECT buid WHERE NOT EXISTS (SELECT 1 FROM s) RETURNING index) SELECT index FROM i UNION ALL select index FROM s $$ LANGUAGE SQL;");
            /* Mutation tables */
            statement.addBatch("CREATE TABLE IF NOT EXISTS mutation (cause_id int REFERENCES users_table (index) NOT NULL, cause_raw int REFERENCES entities_table (index), cause_pos ipos, time timestamp NOT NULL DEFAULT now_utc(), undone boolean NOT NULL DEFAULT false);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS blocks (pos ipos NOT NULL, block int REFERENCES blocks_table (index), action block_action) INHERITS (mutation);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS deaths (target_id int REFERENCES users_table (index) NOT NULL, target_raw int REFERENCES entities_table (index), death_pos dpos NOT NULL) INHERITS (mutation);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS items (inventory_id int REFERENCES users_table (index), inventory_raw int REFERENCES entities_table (index), inventory_pos ipos, data jsonb, action inventory_action) INHERITS (mutation);");
            statement.executeBatch();
        } catch (SQLException sql) {
            throw new PlymouthException(sql, statement, getElseInsertUser, getElseInsertWorld, getElseInsertBlock, getElseInsertEntity, insertBlocks, insertDeaths, insertItems);
        }
    }

    public void sendBatches() {
        if (!queue.isEmpty()) {
            Runnable r = queue.poll();
            while (r != null) try {
                r.run();
            } catch (PlymouthException exception) {
                log.error("Failed to send batch. {}", r, exception);
            } finally {
                r = queue.poll();
            }
            try {
                insertBlocks.executeBatch();
            } catch (SQLException exception) {
                log.error("Failed to send batch.\n{}", insertBlocks, exception);
            }
            try {
                insertDeaths.executeBatch();
            } catch (SQLException exception) {
                log.error("Failed to send batch.\n{}", insertDeaths, exception);
            }
        }
    }

    private boolean areStatesUnnecessary(BlockState state) {
        return state == null || state.getEntries().isEmpty() || state.getBlock() instanceof AbstractFireBlock;
    }

    protected int getBlockIndex(Block block) throws PlymouthException {
        return blocks.computeIfAbsent(block == null ? 0 : block.hashCode(), $ -> {
            try {
                getElseInsertBlock.setObject(1, block == null ? null : Registry.BLOCK.getId(block).toString());
                getElseInsertBlock.setNull(2, Types.NULL);
                var i = getElseInsertBlock.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(getElseInsertBlock, sql);
            }
        });
    }

    protected int getBlockIndex(BlockState state) throws PlymouthException {
        return areStatesUnnecessary(state) ? getBlockIndex(state == null ? null : state.getBlock()) :
                blocks.computeIfAbsent(state.hashCode(), $ -> {
                    try {
                        getElseInsertBlock.setObject(1, Registry.BLOCK.getId(state.getBlock()).toString());
                        getElseInsertBlock.setObject(2, toJson(state.getEntries(), PlymouthHelper.bannedProperties, Property::getName, Objects::toString));
                        var i = getElseInsertBlock.executeQuery();
                        if (!i.next()) throw new SQLException("?!");
                        return i.getInt(1);
                    } catch (SQLException sql) {
                        throw new PlymouthException(getElseInsertBlock, sql);
                    }
                });
    }

    protected int getUserIndex(DamageSource entity) throws PlymouthException {
        return players.computeIfAbsent(Helium.getHash(entity), $ -> {
            try {
                getElseInsertUser.setObject(1, Helium.getName(entity));
                getElseInsertUser.setObject(2, Helium.getUUID(entity));
                var i = getElseInsertUser.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(getElseInsertUser, sql);
            }
        });
    }

    protected int getUserIndex(Entity entity) throws PlymouthException {
        return players.computeIfAbsent(Helium.getHash(entity), $ -> {
            try {
                getElseInsertUser.setObject(1, Helium.getName(entity));
                getElseInsertUser.setObject(2, Helium.getUUID(entity));
                var i = getElseInsertUser.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(getElseInsertUser, sql);
            }
        });
    }

    protected int getEntityIndex(Entity entity) throws PlymouthException {
        return entities.computeIfAbsent(entity.hashCode(), $ -> {
            try {
                getElseInsertEntity.setObject(1, entity.getUuid());
                var i = getElseInsertEntity.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(getElseInsertEntity, sql);
            }
        });
    }

    protected int getWorldIndex(ServerWorld world) throws PlymouthException {
        return worlds.computeIfAbsent(Helium.getHash(world), $ -> {
            try {
                getElseInsertWorld.setObject(1, ((AccessorServerWorld) world).getWorldProperties().getLevelName());
                getElseInsertWorld.setObject(2, world.getRegistryKey().getValue().toString());
                var i = getElseInsertWorld.executeQuery();
                if (!i.next()) throw new SQLException("?!");
                return i.getInt(1);
            } catch (SQLException sql) {
                throw new PlymouthException(getElseInsertWorld, sql);
            }
        });
    }

    private void commonBlockStatement(ServerWorld world, BlockPos pos, int state, Entity entity, String bpu) {
        final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        queue.offer(() -> {
            try {
                insertBlocks.setInt(1, getUserIndex(entity));
                if (!(entity instanceof PlayerEntity) && entity != null)
                    insertBlocks.setInt(2, getEntityIndex(entity));
                else
                    insertBlocks.setNull(2, Types.INTEGER);
                insertBlocks.setInt(3, x);
                insertBlocks.setInt(4, y);
                insertBlocks.setInt(5, z);
                insertBlocks.setInt(6, getWorldIndex(world));
                insertBlocks.setInt(7, state);
                insertBlocks.setObject(8, bpu);
                insertBlocks.addBatch();
            } catch (SQLException sql) {
                throw new PlymouthException(insertBlocks, sql);
            }
        });
    }

    @Override
    public void breakBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity) {
        commonBlockStatement(world, pos, getBlockIndex(state), entity, "BREAK");
    }

    @Override
    public void placeBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity) {
        commonBlockStatement(world, pos, getBlockIndex(state), entity, "PLACE");
    }

    @Override
    public void placeBlock(ServerWorld world, BlockPos pos, Block block, Entity entity) {
        commonBlockStatement(world, pos, getBlockIndex(block), entity, "PLACE");
    }

    @Override
    public void useBlock(ServerWorld world, BlockPos pos, Item i, Entity entity) {
        commonBlockStatement(world, pos, getBlockIndex((Block) null), entity, "USE");
    }

    @Override
    public void replaceBlock(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState, Entity entity) {
        breakBlock(world, pos, oldState, entity);
        placeBlock(world, pos, newState, entity);
    }

    @Override
    public void hurtEntity(LivingEntity target, float amount, DamageSource source) {
        assert target != null;
        if (target.isAlive()) return;
        queue.offer(() -> {
            try {
                insertDeaths.setInt(3, getUserIndex(target));
                if (!(target instanceof PlayerEntity))
                    insertDeaths.setInt(4, getEntityIndex(target));
                else
                    insertDeaths.setNull(4, Types.INTEGER);

                insertDeaths.setInt(1, getUserIndex(source));
                var ce = source.getSource();
                if (!(ce instanceof PlayerEntity) && ce != null)
                    insertDeaths.setInt(2, getEntityIndex(ce));
                else
                    insertDeaths.setNull(2, Types.INTEGER);

                var pos = target.getPos();
                insertDeaths.setDouble(5, pos.x);
                insertDeaths.setDouble(6, pos.y);
                insertDeaths.setDouble(7, pos.z);
                insertDeaths.setInt(8, getWorldIndex((ServerWorld) target.world));
                insertDeaths.addBatch();
            } catch (SQLException sql) {
                throw new PlymouthException(insertDeaths, sql);
            }
        });
    }

    @Override
    public void createEntity(Entity target, Entity creator) {

    }

    @Override
    public void transferItems(BlockPos i, BlockPos o, ItemStack is, int c) {

    }

    @Override
    public void takeItems(BlockPos pos, ItemStack i, int c, Entity taker) {

    }

    @Override
    public void putItems(BlockPos pos, ItemStack i, int c, Entity placer) {

    }

    @Override
    public String getPlayerName(UUID uuid) {
        databaseLock.lock();
        try {
            getUsername.setObject(0, uuid);
            var results = getUsername.executeQuery();
            if (results.first()) {
                // We got a result, return the result from the name column.
                return results.getString("name");
            } else {
                // There's nothing to return, so, return null.
                return null;
            }
        } catch (SQLException sql) {
            throw new PlymouthException(getUsername, sql);
        } finally {
            databaseLock.unlock();
        }
    }
}
