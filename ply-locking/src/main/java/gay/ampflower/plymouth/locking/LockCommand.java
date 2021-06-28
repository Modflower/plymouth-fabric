package gay.ampflower.plymouth.locking;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import gay.ampflower.plymouth.common.InjectableInteractionManager;
import gay.ampflower.plymouth.common.InteractionManagerInjection;
import gay.ampflower.plymouth.locking.handler.AdvancedPermissionHandler;
import gay.ampflower.plymouth.locking.handler.BasicPermissionHandler;
import gay.ampflower.plymouth.locking.handler.IAdvancedPermissionHandler;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Collection;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static gay.ampflower.plymouth.locking.Locking.fromString;
import static gay.ampflower.plymouth.locking.Locking.toText;
import static net.minecraft.command.argument.BlockPosArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.getPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Lock management command for fetching who owns the block, setting who can access the block, and what permissions to give one.
 *
 * @author Ampflower
 * @since 0.0.0
 **/
public class LockCommand {
    @Deprecated(forRemoval = true)
    private static final IntegerArgumentType PERMISSIONS = integer(0, 1 | 2 | 4 | 8);
    private static final SimpleCommandExceptionType
            BLOCK_ENTITY_NOT_FOUND = new SimpleCommandExceptionType(new TranslatableText("commands.data.block.invalid"));
    private static final DynamicCommandExceptionType
            BLOCK_ENTITY_NOT_OWNED = new DynamicCommandExceptionType(i -> new TranslatableText("commands.plymouth.locking.block.not_owned", i)),
            BLOCK_ENTITY_NOT_OWNER = new DynamicCommandExceptionType(i -> new TranslatableText("commands.plymouth.locking.block.not_owner", i)),
            BLOCK_ENTITY_OUT_OF_RANGE = new DynamicCommandExceptionType(i -> new TranslatableText("commands.plymouth.locking.block.out_range", i));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        var add = literal("add")
                .then(argument("players", players())
                        .then(argument("permission", PERMISSIONS)
                                .executes(ctx -> addPlayers(ctx.getSource(), getPlayers(ctx, "players"), getInteger(ctx, "permission"))))
                        .then(argument("pstring", word())
                                .executes(ctx -> addPlayers(ctx.getSource(), getPlayers(ctx, "players"), fromString(getString(ctx, "pstring"))))))
                .then(argument("pos", blockPos()).then(argument("players", players())
                        .then(argument("permission", PERMISSIONS)
                                .executes(ctx -> addPlayers(ctx.getSource(), getBlockPos(ctx, "pos"), getPlayers(ctx, "players"), getInteger(ctx, "permission"))))
                        .then(argument("pstring", word())
                                .executes(ctx -> addPlayers(ctx.getSource(), getBlockPos(ctx, "pos"), getPlayers(ctx, "players"), fromString(getString(ctx, "pstring")))))));
        var rm = literal("remove")
                .then(argument("players", players())
                        .executes(ctx -> removePlayers(ctx.getSource(), getPlayers(ctx, "players"))))
                .then(argument("pos", blockPos()).then(argument("players", players())
                        .executes(ctx -> removePlayers(ctx.getSource(), getBlockPos(ctx, "pos"), getPlayers(ctx, "players")))));
        var set = literal("set")
                .then(argument("permissions", PERMISSIONS).executes(ctx -> setPermissions(ctx.getSource(), getInteger(ctx, "permissions"))))
                .then(argument("pstring", word()).executes(ctx -> setPermissions(ctx.getSource(), fromString(getString(ctx, "pstring")))));
        var get = literal("get")
                .then(argument("pos", blockPos())
                        .executes(ctx -> getLock(ctx.getSource(), getLoadedBlockPos(ctx, "pos"))))
                .executes(ctx -> getLock(ctx.getSource()));
        dispatcher.register(literal("lock").requires(Locking.LOCKING_LOCK_PERMISSION).then(add).then(rm).then(set).then(get));
    }

    // generics are the only thing that can compile this
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity & ILockable> T getBlockEntity(ServerWorld world, BlockPos pos) throws CommandSyntaxException {
        var block = (T) world.getBlockEntity(pos);
        if (block == null) {
            var chunkManager = world.getChunkManager();
            int x = pos.getX() >> 4, z = pos.getZ() >> 4;
            if (!chunkManager.isChunkLoaded(x, z)) {
                var chunk = world.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk != null) block = (T) chunk.getBlockEntity(pos);
            }
            if (block == null) {
                throw BLOCK_ENTITY_NOT_FOUND.create();
            }
        }
        return block;
    }

    private static IPermissionHandler getPermissionHandlerIfAllowedModifyPermissions(ServerCommandSource source, ServerWorld world, BlockPos pos, boolean requiresAdvanced) throws CommandSyntaxException {
        var block = getBlockEntity(world, pos);
        var handler = block.plymouth$getPermissionHandler();
        // If runner == null, and by extension, uuid == anon, we're either the server, a function, or a command block, which can be assumed to be of high privilege.
        if (handler == null) {
            // Auto-own if the block is within reasonable distance of the player, and can be raytraced.
            var runner = source.getPlayer();
            if (runner.world == world && Locking.canReach(runner, block.getPos())) {
                block.plymouth$setPermissionHandler(handler = requiresAdvanced ? new AdvancedPermissionHandler(runner.getUuid()) : new BasicPermissionHandler(runner.getUuid()));
            } else {
                throw BLOCK_ENTITY_OUT_OF_RANGE.create(new TranslatableText(block.getCachedState().getBlock().getTranslationKey()));
            }
        } else {
            if (!handler.allowPermissions(source)) {
                throw BLOCK_ENTITY_NOT_OWNER.create(new TranslatableText(block.getCachedState().getBlock().getTranslationKey()));
            }
            if (requiresAdvanced && !(handler instanceof IAdvancedPermissionHandler)) {
                block.plymouth$setPermissionHandler(handler = new AdvancedPermissionHandler(handler));
            }
        }
        // Premarking for the sake of the handler's ease.
        block.markDirty();
        return handler;
    }

    private static IPermissionHandler getPermissionHandlerIfAllowedModifyPermissions(ServerCommandSource source, BlockPos pos, boolean requiresAdvanced) throws CommandSyntaxException {
        return getPermissionHandlerIfAllowedModifyPermissions(source, source.getWorld(), pos, requiresAdvanced);
    }

    private static int addPlayers(ServerCommandSource source, Collection<ServerPlayerEntity> players, int permission) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayer().interactionManager;
        iim.setManager(new AddPlayersInteractionManager(iim, source, players, (byte) (permission & 0xF)));
        source.sendFeedback(new TranslatableText("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int addPlayers(ServerCommandSource source, BlockPos pos, Collection<ServerPlayerEntity> players, int permission) throws CommandSyntaxException {
        ((IAdvancedPermissionHandler) getPermissionHandlerIfAllowedModifyPermissions(source, pos, true)).addPlayers(players, (byte) (permission & 0xF));
        source.sendFeedback(new TranslatableText("plymouth.locking.allowed", toText(players), new TranslatableText(source.getWorld().getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
        return players.size();
    }

    private static int removePlayers(ServerCommandSource source, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayer().interactionManager;
        iim.setManager(new RemovePlayersInteractionManager(iim, source, players));
        source.sendFeedback(new TranslatableText("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayers(ServerCommandSource source, BlockPos pos, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
        ((IAdvancedPermissionHandler) getPermissionHandlerIfAllowedModifyPermissions(source, pos, true)).removePlayers(players);
        source.sendFeedback(new TranslatableText("plymouth.locking.removed", toText(players), new TranslatableText(source.getWorld().getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
        return players.size();
    }

    private static int setPermissions(ServerCommandSource source, int permissions) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayer().interactionManager;
        iim.setManager(new ModifyPermissionsInteractionManager(iim, source, (short) (permissions & 0xFFF)));
        source.sendFeedback(new TranslatableText("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPermissions(ServerCommandSource source, BlockPos pos, int permissions) throws CommandSyntaxException {
        getPermissionHandlerIfAllowedModifyPermissions(source, pos, false).setPermissions((short) (permissions & 0xFFF));
        return Command.SINGLE_SUCCESS;
    }

    private static int getLock(ServerCommandSource source) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayer().interactionManager;
        iim.setManager(new GetLockInteractionManager(iim, source));
        source.sendFeedback(new TranslatableText("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getLock(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
        var handler = getBlockEntity(source.getWorld(), pos).plymouth$getPermissionHandler();
        if (handler != null) {
            handler.dumpLock(source);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendError(new LiteralText("The selected block is not owned by anyone."));
            return 0;
        }
    }

    private abstract static class InteractionManager implements InteractionManagerInjection {
        protected final InjectableInteractionManager manager;
        protected final ServerCommandSource source;

        private InteractionManager(InjectableInteractionManager manager, ServerCommandSource source) {
            this.manager = manager;
            this.source = source;
        }

        protected abstract void tryInteraction(ServerWorld world, BlockPos pos);

        @Override
        public final ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Direction direction) {
            tryInteraction(world, pos);
            return ActionResult.CONSUME;
        }

        @Override
        public final ActionResult onInteractBlock(ServerPlayerEntity player, ServerWorld world, ItemStack stack, Hand hand, BlockHitResult hitResult) {
            tryInteraction(world, hitResult.getBlockPos());
            return ActionResult.CONSUME;
        }
    }

    private static class RemovePlayersInteractionManager extends InteractionManager {
        private final Collection<ServerPlayerEntity> players;

        private RemovePlayersInteractionManager(InjectableInteractionManager manager, ServerCommandSource source, Collection<ServerPlayerEntity> players) {
            super(manager, source);
            this.players = players;
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                ((IAdvancedPermissionHandler) getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, true)).removePlayers(players);
                source.sendFeedback(new TranslatableText("plymouth.locking.removed", toText(players), new TranslatableText(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text) {
                    source.sendError((Text) msg);
                } else {
                    source.sendError(new LiteralText(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }

    private static class AddPlayersInteractionManager extends InteractionManager {
        private final Collection<ServerPlayerEntity> players;
        private final byte permissions;

        private AddPlayersInteractionManager(InjectableInteractionManager manager, ServerCommandSource source, Collection<ServerPlayerEntity> players, byte permissions) {
            super(manager, source);
            this.players = players;
            this.permissions = permissions;
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                ((IAdvancedPermissionHandler) getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, true)).addPlayers(players, permissions);
                source.sendFeedback(new TranslatableText("plymouth.locking.allowed", toText(players), new TranslatableText(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text) {
                    source.sendError((Text) msg);
                } else {
                    source.sendError(new LiteralText(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }

    private static class ModifyPermissionsInteractionManager extends InteractionManager {
        private final short permissions;

        private ModifyPermissionsInteractionManager(InjectableInteractionManager manager, ServerCommandSource source, short permissions) {
            super(manager, source);
            this.permissions = permissions;
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, false).setPermissions(permissions);
                source.sendFeedback(new TranslatableText("plymouth.locking.modified", "~", new TranslatableText(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text) {
                    source.sendError((Text) msg);
                } else {
                    source.sendError(new LiteralText(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }

    private static class GetLockInteractionManager extends InteractionManager {
        private GetLockInteractionManager(InjectableInteractionManager manager, ServerCommandSource source) {
            super(manager, source);
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                var handler = getBlockEntity(world, pos).plymouth$getPermissionHandler();
                if (handler == null)
                    throw BLOCK_ENTITY_NOT_OWNED.create(new TranslatableText(world.getBlockState(pos).getBlock().getTranslationKey()));
                handler.dumpLock(source);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text) {
                    source.sendError((Text) msg);
                } else {
                    source.sendError(new LiteralText(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }
}
