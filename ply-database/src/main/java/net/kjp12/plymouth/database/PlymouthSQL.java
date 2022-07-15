package net.kjp12.plymouth.database;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.kjp12.hachimitsu.database.api.DatabaseException;
import net.kjp12.hachimitsu.database.api.SqlConnectionProvider;
import net.kjp12.hachimitsu.database.api.StatementCache;
import net.kjp12.plymouth.database.records.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SQL Driver Adaptor. Used for sharing common functions between the many SQL databases out there.
 *
 * @author kjp12
 * @since 0.0.0
 */
public abstract class PlymouthSQL implements Plymouth, SqlConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(PlymouthSQL.class);

    private int ticks = 0;
    private final ItemStack[] immutablesCache = new ItemStack[256];
    private final Map<TargetRecord, Map<TargetRecord, Object2ObjectMap<ItemStack, InventoryRecord>>> inventoryDeltaTable = new HashMap<>();

    protected Driver driver;
    protected Connection connection;

    protected StatementCache<BlockLookupRecord> blockLookupCache;
    protected StatementCache<DeathLookupRecord> deathLookupCache;
    protected StatementCache<InventoryLookupRecord> inventoryLookupCache;

    protected PreparedStatement
            insertBlocks, insertDeaths, insertItems,
            getElseInsertUser, getElseInsertBlock, getElseInsertWorld, getUsername;
    protected final ReentrantLock databaseLock = new ReentrantLock();

    private final Queue<PlymouthRecord> queue = new ConcurrentLinkedQueue<>();

    protected PlymouthSQL(Driver driver) {
        this.driver = driver;
    }

    protected PlymouthSQL(Driver driver, String uri, Properties properties) throws PlymouthException {
        this(driver);
        startConnection(uri, properties);
    }

    public void startConnection(String uri, Properties properties) throws PlymouthException {
        try {
            this.connection = driver.connect(uri, properties);
            connection.isClosed();
        } catch (SQLException sql) {
            throw new PlymouthException(sql);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This method may block as it has to synchronise on {@link #inventoryDeltaTable} to ensure integrity.
     * This also includes iterating over the table and executing requests and clearing up empty maps.
     */
    @Override
    public void sendBatches() {
        // This determines if we should continue on to finish off batch requests from inventories.
        boolean f = false;
        // Test if we should process the inventory delta table. If so, synchronise.
        if ((ticks++ & 127) == 0) synchronized (inventoryDeltaTable) {
            var i0 = inventoryDeltaTable.entrySet().iterator();
            while (i0.hasNext()) {
                var entry = i0.next();
                var v0 = entry.getValue();
                // We don't remove right away, but rather wait a moment before clearing, in case it gets reused immediately.
                if (v0.isEmpty()) {
                    i0.remove();
                    continue;
                }
                var itr3 = v0.entrySet().iterator();
                while (itr3.hasNext()) {
                    var e3 = itr3.next();
                    var v3 = e3.getValue();
                    // Same reasoning as before; we wait in case it gets reused immediately.
                    if (v3.isEmpty()) {
                        itr3.remove();
                        continue;
                    }
                    var itr2 = Object2ObjectMaps.fastIterator(v3);
                    while (itr2.hasNext()) {
                        var entry2 = itr2.next();
                        var record = entry2.getValue();
                        try {
                            handleInventoryRecord(record);
                            f = true;
                        } catch (IllegalStateException | NullPointerException | PlymouthException | SQLException exception) {
                            log.error("Failed to add record {} to batch.\n{}", record, insertItems, exception);
                        }
                        itr2.remove();
                    }
                }
            }
        }
        if (f || !queue.isEmpty()) {
            PlymouthRecord r;
            PreparedStatement lastStatement = null;
            while ((r = queue.poll()) != null) try {
                switch (r.getType()) {
                    case BLOCK -> {
                        lastStatement = insertBlocks;
                        handleBlockRecord((BlockRecord) r);
                    }
                    case DEATH -> {
                        lastStatement = insertDeaths;
                        handleDeathRecord((DeathRecord) r);
                    }
                    case INVENTORY -> {
                        lastStatement = insertItems;
                        handleInventoryRecord((InventoryRecord) r);
                    }
                    case LOOKUP_BLOCK -> blockLookupCache.handle((BlockLookupRecord) r);
                    case LOOKUP_DEATH -> deathLookupCache.handle((DeathLookupRecord) r);
                    case LOOKUP_INVENTORY -> inventoryLookupCache.handle((InventoryLookupRecord) r);
                    default -> log.warn("Unknown type {} for record {}.", r.getType(), r);
                }
            } catch (IllegalStateException | NullPointerException | PlymouthException | DatabaseException | SQLException exception) {
                if (r instanceof CompletableRecord<?> completable) completable.fail(exception);
                log.error("Failed to add record {} to batch.\n{}", r, lastStatement, exception);
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
            try {
                insertItems.executeBatch();
            } catch (SQLException exception) {
                log.error("Failed to send batch.\n{}", insertItems, exception);
            }
        }
    }

    @Override
    public void queue(PlymouthRecord record) {
        queue.offer(record);
    }

    /**
     * Inserts a block record into the database.
     *
     * @param blockRecord The block record to submit.
     * @throws SQLException      If the database fails, i.e. bad SQL, and is uncaught.
     * @throws PlymouthException If anything fails, giving extra diagnostic information.
     */
    protected abstract void handleBlockRecord(BlockRecord blockRecord) throws SQLException, PlymouthException;

    /**
     * Inserts a death record into the database.
     *
     * @param deathRecord The death record to submit.
     * @throws SQLException      If the database fails, i.e. bad SQL, and is uncaught.
     * @throws PlymouthException If anything fails, giving extra diagnostic information.
     */
    protected abstract void handleDeathRecord(DeathRecord deathRecord) throws SQLException, PlymouthException;

    /**
     * Inserts an inventory record into the database.
     *
     * @param inventoryRecord The inventory record to submit.
     * @throws SQLException      If the database fails, i.e. bad SQL, and is uncaught.
     * @throws PlymouthException If anything fails, giving extra diagnostic information.
     */
    protected abstract void handleInventoryRecord(InventoryRecord inventoryRecord) throws SQLException, PlymouthException;

    /**
     * Looks up the integer index for a given block state. Must <em>not</em> be called asynchronously.
     *
     * @param state The block state to look up.
     * @return The integer index of a given block state within the database.
     * @throws PlymouthException If the lookup fails, giving the SQL error and the statement.
     */
    protected abstract int getBlockIndex(BlockState state) throws PlymouthException;

    @Override
    public void breakBlock(ServerWorld world, BlockPos pos, BlockState state, NbtCompound nbt, @Nullable Target cause) {
        if (cause != null) queue(new BlockRecord(cause, world, pos, BlockAction.BREAK, state, nbt));
    }

    @Override
    public void placeBlock(ServerWorld world, BlockPos pos, BlockState state, @Nullable Target cause) {
        if (cause != null) queue(new BlockRecord(cause, world, pos, BlockAction.PLACE, state));
    }

    @Override
    public void useBlock(ServerWorld world, BlockPos pos, Item i, @Nullable Target user) {
        if (user != null) queue(new BlockRecord(user, world, pos, BlockAction.USE, null));
    }

    @Override
    public void replaceBlock(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState, @Nullable Target entity) {
        breakBlock(world, pos, oldState, null, entity);
        placeBlock(world, pos, newState, entity);
    }

    @Override
    public void killEntity(Target target, Target source) {
        assert target != null;
        assert source != null;
        queue(new DeathRecord(source, target));
    }

    @Override
    public void createEntity(Entity target, Entity creator) {

    }

    @Override
    public void takeItems(Target inventory, ItemStack i, int c, @Nullable Target taker) {
        if (taker != null && !taker.ply$targetMatches(inventory))
            inventoryDelta(inventory.plymouth$toRecord(), taker.plymouth$toRecord(), i, -c);
    }

    @Override
    public void putItems(Target inventory, ItemStack i, int c, @Nullable Target placer) {
        if (placer != null && !placer.ply$targetMatches(inventory))
            inventoryDelta(inventory.plymouth$toRecord(), placer.plymouth$toRecord(), i, c);
    }

    /**
     * Handles inventory records in an accumulative manner to prevent several
     * records from being created when only one with a large delta suffices.
     *
     * @param inventory The inventory items were added or removed from.
     * @param mutator   The mutator of the inventory.
     * @param reference The stack for reference. A copy treated as immutable will be created.
     * @param delta     How much was added or removed from the inventory.
     * @implNote This method may block as it has to synchronise on {@link #inventoryDeltaTable} to ensure integrity.
     */
    private void inventoryDelta(TargetRecord inventory, TargetRecord mutator, ItemStack reference, int delta) {
        // The following three assertions are for dev-time to detect where the accidental insertion of air or 0 are coming from.
        assert delta != 0 : "No delta.";
        assert reference != null && !reference.isEmpty() : "Air got in the system.";
        assert !(reference.getItem() instanceof BlockItem blockItem && blockItem.getBlock().getDefaultState().isAir()) : "Unusual air got in the system.";
        synchronized (inventoryDeltaTable) {
            var map = getRecordMap(inventory, mutator);
            var record = map.get(reference);
            if (record == null) {
                var immutable = immutable(reference);
                record = new InventoryRecord(mutator, inventory, immutable, delta);
                map.put(immutable, record);
            } else {
                record.delta += delta;
            }
        }
    }

    /**
     * Fetches or creates an ItemStack to Record map based off of the inventory and mutator.
     *
     * @param inventory The inventory to base the first fetch off of.
     * @param mutator   The mutator to base the second fetch off of.
     * @return The hashmap tuned for item stacks.
     */
    private Map<ItemStack, InventoryRecord> getRecordMap(TargetRecord inventory, TargetRecord mutator) {
        return inventoryDeltaTable.computeIfAbsent(inventory, $ -> new HashMap<>())
                .computeIfAbsent(mutator, $ -> new Object2ObjectOpenCustomHashMap<>(ItemStackHasher.INSTANCE));
    }

    /**
     * Fetches or creates a copy of the input stack to treat as immutable.
     *
     * @param stack The stack to base off of or to fetch by.
     * @return The copy of the stack to treat as immutable, with the amount set to 1.
     * @implNote This is not synchronised as this simple lookup table does not need to maintain integrity,
     * as if the item does not match, it'll just overwrite the entry then return.
     */
    // Well, it's not really immutable, but we treat it as if it was.
    private ItemStack immutable(ItemStack stack) {
        var h = ItemStackHasher.INSTANCE.hashCode(stack) & 255;
        var s = immutablesCache[h];
        if (ItemStackHasher.INSTANCE.equals(s, stack)) {
            return s;
        }
        var c = stack.copy();
        c.setCount(1);
        return immutablesCache[h] = c;
    }
}
