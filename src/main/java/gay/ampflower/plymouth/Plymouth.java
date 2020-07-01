package gay.ampflower.plymouth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Plymouth Database Driver Adaptor, a meta-driver for driving databases.
 * <p>
 * Currently supports {@link PlymouthPostgres PostgreSQL} and {@link PlymouthNoOP No-OP} drivers.
 *
 * @author Ampflower
 * @since 0.0.0
 */
public interface Plymouth {
    void initializeDatabase() throws PlymouthException;

    void sendBatches();

    /**
     * @param world  The world the block was broken in.
     * @param pos    Where the block got broken.
     * @param state  The old blockstate.
     * @param entity Who broke this?
     */
    void breakBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity);

    /**
     * @param world  The world the block was placed in.
     * @param pos    Where the block got placed.
     * @param state  The new blockstate.
     * @param entity Who placed this?
     */
    void placeBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity);

    /**
     * Same as {@link #placeBlock(ServerWorld, BlockPos, BlockState, Entity)} but for when blockstates are meaningless, ie. FIRE.
     *
     * @param world  The world the block was placed in.
     * @param pos    Where the block got placed.
     * @param block  The new block.
     * @param entity Who placed this?
     */
    void placeBlock(ServerWorld world, BlockPos pos, Block block, Entity entity);

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
    void useBlock(ServerWorld world, BlockPos pos, Item i, Entity user);

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
    void replaceBlock(ServerWorld world, BlockPos pos, BlockState o, BlockState n, Entity replacer);

    void hurtEntity(LivingEntity target, float amount, DamageSource source);

    void createEntity(Entity target, Entity creator);

    void transferItems(BlockPos i, BlockPos o, ItemStack is, int c);

    void takeItems(BlockPos pos, ItemStack i, int c, Entity taker);

    void putItems(BlockPos pos, ItemStack i, int c, Entity placer);
}
