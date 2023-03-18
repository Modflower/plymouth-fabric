package net.kjp12.plymouth.common;// Created 2021-03-27T22:47:11

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Callback from the {@link net.minecraft.server.network.ServerPlayerInteractionManager} that intercepts the request
 * at the earliest it can, and if consumed, failed or successful, will block the action itself from occurring,
 * possibly replacing the blocked action with its own.
 *
 * @author Ampflower
 * @see InjectableInteractionManager#setManager(InteractionManagerInjection)
 * @since 0.0.0
 */
public interface InteractionManagerInjection {
    /**
     * Event for when blocks are being broken.
     *
     * @param player    The player attempting to break a block.
     * @param world     The world the player's in.
     * @param pos       The position of the block.
     * @param direction The direction the block's getting broken at.
     * @return if the action should pass, be consumed, or if it was successful.
     */
    ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Direction direction);

    /**
     * Event for when blocks are being used.
     *
     * @param player    The player attempting to use a block.
     * @param world     The world the player's in.
     * @param stack     The item stack in the player's hand.
     * @param hand      The hand that activated the request.
     * @param hitResult The result of the use.
     * @return if the action should pass, be consumed, or if it was successful.
     */
    ActionResult onInteractBlock(ServerPlayerEntity player, ServerWorld world, ItemStack stack, Hand hand, BlockHitResult hitResult);
}
