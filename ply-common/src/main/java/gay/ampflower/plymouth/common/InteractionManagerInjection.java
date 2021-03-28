package gay.ampflower.plymouth.common;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public interface InteractionManagerInjection {
    ActionResult onBreakBlock(BlockPos pos);

    ActionResult onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult);
}
