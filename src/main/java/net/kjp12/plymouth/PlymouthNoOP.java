package net.kjp12.plymouth;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class PlymouthNoOP implements Plymouth {
    public void initializeDatabase() {
    }

    public void sendBatches() {
    }

    public void breakBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity) {
    }

    public void placeBlock(ServerWorld world, BlockPos pos, BlockState state, Entity entity) {
    }

    public void placeBlock(ServerWorld world, BlockPos pos, Block block, Entity entity) {
    }

    public void useBlock(ServerWorld world, BlockPos pos, Item w, Entity user) {
    }

    public void replaceBlock(ServerWorld world, BlockPos pos, BlockState o, BlockState n, Entity replacer) {
    }

    public void hurtEntity(LivingEntity target, float amount, DamageSource source) {
    }

    public void createEntity(Entity target, Entity creator) {
    }

    public void transferItems(BlockPos i, BlockPos o, ItemStack is, int c) {
    }

    public void takeItems(BlockPos pos, ItemStack i, int c, Entity taker) {
    }

    public void putItems(BlockPos pos, ItemStack i, int c, Entity placer) {
    }
}
