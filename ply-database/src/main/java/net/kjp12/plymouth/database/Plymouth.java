package net.kjp12.plymouth.database;

import net.kjp12.plymouth.database.records.PlymouthRecord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Plymouth Database Driver Adaptor, a meta-driver for driving databases.
 * <p>
 * Currently supports {@link PlymouthPostgres PostgreSQL} and {@link PlymouthNoOP No-OP} drivers.
 *
 * @author kjp12
 * @since 0.0.0
 */
public interface Plymouth {
    /**
     * Either initialises or migrates the database for use.
     * <p>
     * Normally includes any driver setup.
     *
     * @throws PlymouthException When any failure with setup occurs.
     */
    void initializeDatabase() throws PlymouthException;

    /**
     * Instructs the database to send all batches.
     * <p>
     * This includes the following.
     * <ul>
     *     <li>Blocks</li>
     *     <li>Deaths</li>
     *     <li>Inventories</li>
     * </ul>
     * <p>
     * Additional handling of lookups and special handling of accumulative records such as the
     * {@link net.kjp12.plymouth.database.records.InventoryRecord Inventory Record} may also occur here.
     * <p>
     * This must <em>not</em> to be called asynchronously.
     */
    void sendBatches();

    /**
     * Queues a record for insertion or query by the database.
     *
     * @param record The record to queue.
     */
    void queue(PlymouthRecord record);

    /**
     * @param world The world the block was broken in.
     * @param pos   Where the block got broken.
     * @param state The old blockstate.
     * @param nbt   The NBT of the block pre-removal.
     * @param cause Who broke this?
     */
    void breakBlock(ServerWorld world, BlockPos pos, BlockState state, NbtCompound nbt, @Nullable Target cause);

    /**
     * @param world The world the block was placed in.
     * @param pos   Where the block got placed.
     * @param state The new blockstate.
     * @param cause Who placed this?
     */
    void placeBlock(ServerWorld world, BlockPos pos, BlockState state, @Nullable Target cause);

    /**
     * Same as {@link #placeBlock(ServerWorld, BlockPos, BlockState, Target)} but for when blockstates are meaningless, ie. FIRE.
     *
     * @param world The world the block was placed in.
     * @param pos   Where the block got placed.
     * @param block The new block.
     * @param cause Who placed this?
     */
    default void placeBlock(ServerWorld world, BlockPos pos, Block block, @Nullable Target cause) {
        placeBlock(world, pos, block.getDefaultState(), cause);
    }

    /**
     * Changes such as tuning noteblocks and timings should be committed with a number.
     * Anything that doesn't require an item should not send one.
     * <p>
     * Anything that doesn't permanently change a block state, ie. a bell, shouldn't commit.
     *
     * @param world The world the block was used in.
     * @param pos   Where the block got used.
     * @param i     The item that was used.
     * @param user  Who used this?
     */
    void useBlock(ServerWorld world, BlockPos pos, Item i, @Nullable Target user);

    /**
     * Redstone components, such as redstone dust, dispensers, droppers, observers and anything else with a state change
     * will not be logged through this. The amount of changes through this may overwhelm the database when committed.
     *
     * @param world    The dimension the block was replaced in.
     * @param pos      Where the block got replaced.
     * @param o        The old blockstate.
     * @param n        The new blockstate.
     * @param replacer Who replaced this?
     */
    void replaceBlock(ServerWorld world, BlockPos pos, BlockState o, BlockState n, @Nullable Target replacer);

    void hurtEntity(LivingEntity target, float amount, DamageSource source);

    void createEntity(Entity target, Entity creator);

    void takeItems(Target inventory, ItemStack stack, int count, @Nullable Target taker);

    void putItems(Target inventory, ItemStack stack, int count, @Nullable Target placer);

    /**
     * Player lookup by database.
     *
     * @param uuid The UUID to look up by.
     * @return The username if it exists in the database, else null.
     * @throws PlymouthException Request has failed, either by closed driver or bad SQL.
     */
    String getPlayerName(UUID uuid) throws PlymouthException;
}
