package gay.ampflower.plymouth.database;

import gay.ampflower.plymouth.database.records.CompletableRecord;
import gay.ampflower.plymouth.database.records.PlymouthRecord;
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
 * No operation plymouth database driver. Any completable will instantly fail with an unsupported operation exception.
 *
 * @author Ampflower
 * @since ${version}
 */
public class PlymouthNoOP implements Plymouth {
    public void initializeDatabase() {
    }

    public void sendBatches() {
    }

    public void queue(PlymouthRecord record) {
        if (record instanceof CompletableRecord<?> completable) {
            completable.fail(new UnsupportedOperationException("no-op: record unsupported"));
        }
    }

    public void breakBlock(ServerWorld world, BlockPos pos, BlockState state, NbtCompound nbt, @Nullable Target cause) {
    }

    public void placeBlock(ServerWorld world, BlockPos pos, BlockState state, @Nullable Target cause) {
    }

    public void placeBlock(ServerWorld world, BlockPos pos, Block block, @Nullable Target cause) {
    }

    public void useBlock(ServerWorld world, BlockPos pos, Item w, @Nullable Target user) {
    }

    public void replaceBlock(ServerWorld world, BlockPos pos, BlockState o, BlockState n, @Nullable Target replacer) {
    }

    public void killEntity(Target target, Target source) {
    }

    public void hurtEntity(LivingEntity target, float amount, DamageSource source) {
    }

    public void createEntity(Entity target, Entity creator) {
    }

    public void takeItems(Target inventory, ItemStack i, int c, @Nullable Target mutator) {
    }

    public void putItems(Target inventory, ItemStack i, int c, @Nullable Target mutator) {
    }

    public String getPlayerName(UUID uuid) {
        return null;
    }
}
