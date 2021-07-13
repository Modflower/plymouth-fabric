/* Copyright (c) 2021 Ampflower
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package gay.ampflower.helium.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class HotspotCommand {
    public static final Predicate<ServerCommandSource> REQUIRE_HOTSPOT_PERMISSION = Permissions
            .require("plymouth.admin.moderation.hotspot", 2);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("hotspot").requires(REQUIRE_HOTSPOT_PERMISSION)
                .then(literal("entities").executes(HotspotCommand::entities)));
    }

    private static int entities(CommandContext<ServerCommandSource> context) {
        var primary = new HashMap<EntityType<?>, Long2ObjectOpenHashMap<EntityRecord>>();
        var source = context.getSource();
        var server = source.getMinecraftServer();
        for (var world : server.getWorlds()) {
            primary.clear();
            for (var entity : world.iterateEntities()) {
                primary.computeIfAbsent(entity.getType(), $0 -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(packBlockPosAsChunkPos(entity.getBlockPos()), $0 -> new EntityRecord())
                        .add(entity.getPos());
            }

        }
        return Command.SINGLE_SUCCESS;
    }

    private static long packBlockPosAsChunkPos(BlockPos pos) {
        return BlockPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    private static class EntityRecord {
        double x, y, z;
        int c;

        void add(Vec3d v) {
            x += v.x;
            y += v.y;
            z += v.z;
            c++;
        }
    }
}
