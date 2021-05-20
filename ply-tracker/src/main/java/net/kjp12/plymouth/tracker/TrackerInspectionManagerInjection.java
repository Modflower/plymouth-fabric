package net.kjp12.plymouth.tracker;// Created 2021-06-04T03:21

import net.kjp12.plymouth.common.InteractionManagerInjection;
import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.records.LookupRecord;
import net.kjp12.plymouth.database.records.PlymouthRecord;
import net.kjp12.plymouth.database.records.RecordType;
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
 * @author KJP12
 * @since ${version}
 **/
public class TrackerInspectionManagerInjection implements InteractionManagerInjection {
    @Override
    public ActionResult onBreakBlock(ServerPlayerEntity player, final ServerWorld world, final BlockPos pos, Direction direction) {
        var future = new CompletableFuture<List<PlymouthRecord>>();
        DatabaseHelper.database.queue(new LookupRecord(future, RecordType.BLOCK, world, pos, 0));
        future.thenAcceptAsync(l -> {
            for (var r : l) {
                player.sendMessage(r.toTextNoPosition(), false);
            }
            player.sendMessage(new TranslatableText("commands.plymouth.tracker.inspect.lookup", pos.getX(), pos.getY(), pos.getZ(), "UTC").formatted(Formatting.DARK_GRAY), false);
        }).exceptionally(t -> {
            player.sendMessage(new LiteralText(t.getLocalizedMessage()).formatted(Formatting.RED), false);
            return null;
        });
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult onInteractBlock(ServerPlayerEntity player, ServerWorld world, ItemStack stack, Hand hand, BlockHitResult hitResult) {
        var future = new CompletableFuture<List<PlymouthRecord>>();
        var pos = hitResult.getBlockPos();
        DatabaseHelper.database.queue(new LookupRecord(future, RecordType.INVENTORY, world, pos, 0));
        future.thenAcceptAsync(l -> {
            for (var r : l) {
                player.sendMessage(r.toTextNoPosition(), false);
            }
            player.sendMessage(new TranslatableText("commands.plymouth.tracker.inspect.lookup", pos.getX(), pos.getY(), pos.getZ(), "UTC").formatted(Formatting.DARK_GRAY), false);
        }).exceptionally(t -> {
            player.sendMessage(new LiteralText(t.getLocalizedMessage()).formatted(Formatting.RED), false);
            return null;
        });
        return ActionResult.CONSUME;
    }
}
