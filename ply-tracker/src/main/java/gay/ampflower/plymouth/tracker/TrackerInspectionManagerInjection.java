package gay.ampflower.plymouth.tracker;

import gay.ampflower.plymouth.common.InteractionManagerInjection;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.records.BlockLookupRecord;
import gay.ampflower.plymouth.database.records.InventoryLookupRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class TrackerInspectionManagerInjection implements InteractionManagerInjection {
    @Override
    public ActionResult onBreakBlock(ServerPlayerEntity player, final ServerWorld world, final BlockPos pos, Direction direction) {
        var lookup = new BlockLookupRecord(world, pos, 0);
        DatabaseHelper.database.queue(lookup);
        lookup.getFuture().thenAcceptAsync(l -> {
            for (var r : l) {
                player.sendMessage(r.toTextNoPosition(), false);
            }
            player.sendMessage(new TranslatableText("commands.plymouth.tracker.lookup", lookup.toText(), "UTC").formatted(Formatting.DARK_GRAY), false);
        }).exceptionally(t -> {
            player.sendMessage(new LiteralText(t.getLocalizedMessage()).formatted(Formatting.RED), false);
            return null;
        });
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult onInteractBlock(ServerPlayerEntity player, ServerWorld world, ItemStack stack, Hand hand, BlockHitResult hitResult) {
        if (hand != Hand.MAIN_HAND) return ActionResult.CONSUME;
        var lookup = new InventoryLookupRecord(world, hitResult.getBlockPos(), 0);
        DatabaseHelper.database.queue(lookup);
        lookup.getFuture().thenAcceptAsync(l -> {
            for (var r : l) {
                player.sendMessage(r.toTextNoPosition(), false);
            }
            player.sendMessage(new TranslatableText("commands.plymouth.tracker.lookup", lookup.toText(), "UTC").formatted(Formatting.DARK_GRAY), false);
        }).exceptionally(t -> {
            player.sendMessage(new LiteralText(t.getLocalizedMessage()).formatted(Formatting.RED), false);
            return null;
        });
        return ActionResult.CONSUME;
    }
}
