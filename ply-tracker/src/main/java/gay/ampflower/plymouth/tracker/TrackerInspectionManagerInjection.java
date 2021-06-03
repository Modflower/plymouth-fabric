package gay.ampflower.plymouth.tracker;

import gay.ampflower.plymouth.common.InteractionManagerInjection;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.records.LookupRecord;
import gay.ampflower.plymouth.database.records.PlymouthRecord;
import gay.ampflower.plymouth.database.records.RecordType;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class TrackerInspectionManagerInjection implements InteractionManagerInjection {
    @Override
    public ActionResult onBreakBlock(ServerPlayerEntity player, final ServerWorld world, final BlockPos pos, Direction direction) {
        var future = new CompletableFuture<List<PlymouthRecord>>();
        var lookup = new LookupRecord(future, RecordType.BLOCK, world, pos, 0);
        DatabaseHelper.database.queue(lookup);
        future.thenAcceptAsync(l -> {
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
        var future = new CompletableFuture<List<PlymouthRecord>>();
        var lookup = new LookupRecord(future, RecordType.INVENTORY, world, hitResult.getBlockPos(), 0);
        DatabaseHelper.database.queue(lookup);
        future.thenAcceptAsync(l -> {
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
