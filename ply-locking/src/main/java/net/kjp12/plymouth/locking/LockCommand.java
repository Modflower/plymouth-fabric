package net.kjp12.plymouth.locking;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kjp12.plymouth.common.InjectableInteractionManager;
import net.kjp12.plymouth.common.InteractionManagerInjection;
import net.kjp12.plymouth.locking.handler.AdvancedPermissionHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkStatus;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.kjp12.plymouth.locking.Locking.*;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.command.argument.EntityArgumentType.getPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;

/**
 * Lock management command for fetching who owns the block, setting who can access the block, and what permissions to give one.
 *
 * @author Ampflower
 * @since 0.0.0
 **/
public class LockCommand {
    /**
     * The argument field in Brigadier's {@link CommandContext}. Required as Mixin doesn't work on that class.
     */
    private static final Field brigadierArguments;
    /**
     * The primitive to wrapper map. Required as we cannot mixin into {@link CommandContext}.
     */
    private static final Map<Class<?>, Class<?>> primitiveToWrapper;
    private static final SimpleCommandExceptionType
            BLOCK_ENTITY_NOT_FOUND = new SimpleCommandExceptionType(translatable("commands.data.block.invalid"));
    private static final DynamicCommandExceptionType
            BLOCK_ENTITY_NOT_OWNED = new DynamicCommandExceptionType(i -> translatable("commands.plymouth.locking.block.not_owned", i)),
            BLOCK_ENTITY_NOT_OWNER = new DynamicCommandExceptionType(i -> translatable("commands.plymouth.locking.block.not_owner", i)),
            BLOCK_ENTITY_OUT_OF_RANGE = new DynamicCommandExceptionType(i -> translatable("commands.plymouth.locking.block.out_range", i));

    static {
        // Fetches the arguments field instance for Brigadier's CommandContext.
        try {
            var arguments = CommandContext.class.getDeclaredField("arguments");
            arguments.setAccessible(true);
            brigadierArguments = arguments;
        } catch (ReflectiveOperationException roe) {
            // We cannot continue as it's critical; fail with a descriptive error.
            throw new LinkageError("""
                    Unable to fetch the arguments field from Brigadier.
                    Please update Plymouth Locking to the latest version, if any updates are available.
                    If the bug persists, file a bug report at https://github.com/the-glitch-network/plymouth-fabric/issues
                    """, roe);
        }
        // The primitive map. HotSpot's implementation as of 17.0.1
        // does seem well optimised for this use.
        primitiveToWrapper = Map.of(
                void.class, Void.class,
                byte.class, Byte.class,
                char.class, Character.class,
                short.class, Short.class,
                int.class, Integer.class,
                long.class, Long.class,
                float.class, Float.class,
                double.class, Double.class
        );
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var trust = dispatcher.register(literal("trust").requires(LOCKING_LOCK_PERMISSION)
                .then(argument("players", players())
                        .executes(LockCommand::modifyPlayer)
                        .then(argument("permissions", greedyString())
                                .executes(LockCommand::modifyPlayer))
                ));

        var distrust = dispatcher.register(literal("distrust").requires(LOCKING_LOCK_PERMISSION)
                .then(argument("players", players())
                        .executes(LockCommand::removePlayer)
                ));

        var at = literal("at").then(argument("position", blockPos())
                .then(literal("player")
                        .then(literal("add").redirect(trust))
                        .then(literal("modify").redirect(trust))
                        .then(literal("trust").redirect(trust))
                        .then(literal("remove").redirect(distrust)))
                .then(literal("modify").then(argument("permissions", greedyString())
                        .executes(ctx -> setPermissions(ctx.getSource(), getBlockPos(ctx, "position"), fromString(getString(ctx, "permissions"))))))
                .then(literal("get").executes(ctx -> getLock(ctx.getSource(), getBlockPos(ctx, "position")))));

        var interact = literal("interact")
                .then(literal("player")
                        .then(literal("add").redirect(trust))
                        .then(literal("modify").redirect(trust))
                        .then(literal("trust").redirect(trust))
                        .then(literal("remove").redirect(distrust)))
                .then(literal("modify").then(argument("permissions", greedyString())
                        .executes(ctx -> setPermissions(ctx.getSource(), fromString(getString(ctx, "permissions"))))))
                .then(literal("get").executes(ctx -> getLock(ctx.getSource())));


        dispatcher.register(literal("lock").requires(Locking.LOCKING_LOCK_PERMISSION).then(at).then(interact));
    }

    /**
     * Modify/add player implementation expecting {@code players} argument,
     * with optional {@code permissions} and {@code position} arguments to allow for reuse.
     *
     * @param ctx The command context for fetching arguments from.
     * @return 1 if the command was successful, 0 otherwise
     * @throws CommandSyntaxException If the command fails due to bad arguments.
     */
    private static int modifyPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        var players = getPlayers(ctx, "players");
        var permissions = fromString(getArgumentOptional(ctx, "permissions", String.class, "rw"));
        var pos = getArgumentOptional(ctx, "position", PosArgument.class, null);
        if (pos != null) {
            return addPlayers(src, pos.toAbsoluteBlockPos(src), players, permissions);
        } else {
            return addPlayers(src, players, permissions);
        }
    }

    /**
     * Remove player implementation expecting {@code players} argument,
     * with optional {@code position} argument to allow for reuse.
     *
     * @param ctx The command context for fetching arguments from.
     * @return 1 if the command was successful, 0 otherwise.
     * @throws CommandSyntaxException If the command fails due to bad arguments.
     */
    private static int removePlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var src = ctx.getSource();
        var players = getPlayers(ctx, "players");
        var pos = getArgumentOptional(ctx, "position", PosArgument.class, null);
        if (pos != null) {
            return removePlayers(src, pos.toAbsoluteBlockPos(src), players);
        } else {
            return removePlayers(src, players);
        }
    }

    /**
     * Gets a named argument from the command context, or null if undefined.
     * <p>
     * Reimplements {@link CommandContext#getArgument(String, Class)} with
     * the ability to return a default or null.
     *
     * @param name  The name of the argument
     * @param clazz The type of the argument
     * @param def   The default return, maybe null.
     * @return The argument as parsed by Brigadier if existing, default to {@code def} otherwise.
     * @throws RuntimeException         If reflection fails. This should never occur normally.
     * @throws IllegalArgumentException If the argument isn't of the expected class.
     */
    @SuppressWarnings("unchecked")
    public static <V, S, P extends ParsedArgument<S, ?>, ARGS extends Map<String, P>> V getArgumentOptional(CommandContext<S> ctx, String name, Class<V> clazz, V def) {
        P arg;
        try {
            arg = ((ARGS) brigadierArguments.get(ctx)).get(name);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Unable to get arguments.", iae);
        }

        if (arg == null) {
            return def;
        }

        final Object result = arg.getResult();
        if (primitiveToWrapper.getOrDefault(clazz, clazz).isAssignableFrom(result.getClass())) {
            return (V) result;
        } else {
            throw new IllegalArgumentException("Argument '" + name + "' is defined as " + result.getClass().getSimpleName() + ", not " + clazz);
        }
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

    private static LockDelegate getPermissionHandlerIfAllowedModifyPermissions(ServerCommandSource source, ServerWorld world, BlockPos pos, boolean requiresAdvanced) throws CommandSyntaxException {
        var handler = Locking.surrogate(world, pos, source);
        // If runner == null, and by extension, uuid == anon, we're either the server, a function, or a command block, which can be assumed to be of high privilege.
        if (!handler.plymouth$isOwned()) {
            // Auto-own if the block is within reasonable distance of the player, and can be raytraced.
            var runner = source.getPlayerOrThrow();
            if (runner.world == world && Locking.canReach(runner, pos)) {
                if (requiresAdvanced) {
                    handler.claimAdvanced(runner.getUuid());
                } else {
                    handler.claim(runner.getUuid());
                }
            } else {
                throw BLOCK_ENTITY_OUT_OF_RANGE.create(translatable(handler.getBlock().getTranslationKey()));
            }
        } else {
            if ((handler.effective() & PERMISSIONS_PERMISSION) == 0) {
                throw BLOCK_ENTITY_NOT_OWNER.create(translatable(handler.getBlock().getTranslationKey()));
            }
            if (requiresAdvanced) {
                handler.compute(iph -> iph instanceof AdvancedPermissionHandler aph ? aph : new AdvancedPermissionHandler(iph));
            }
        }
        // Premarking for the sake of the handler's ease.
        handler.markDirty();
        return handler;
    }

    private static LockDelegate getPermissionHandlerIfAllowedModifyPermissions(ServerCommandSource source, BlockPos pos, boolean requiresAdvanced) throws CommandSyntaxException {
        return getPermissionHandlerIfAllowedModifyPermissions(source, source.getWorld(), pos, requiresAdvanced);
    }

    private static int addPlayers(ServerCommandSource source, Collection<ServerPlayerEntity> players, int permission) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayerOrThrow().interactionManager;
        iim.setManager(new AddPlayersInteractionManager(iim, source, players, (short) (permission & 0xFF | ((permission >>> 12) & 0xFF00))));
        source.sendFeedback(translatable("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int addPlayers(ServerCommandSource source, BlockPos pos, Collection<ServerPlayerEntity> players, int permission) throws CommandSyntaxException {
        getPermissionHandlerIfAllowedModifyPermissions(source, pos, true).modifyPlayers(players, (short) (permission & 0xFF | ((permission >>> 12) & 0xFF00)));
        source.sendFeedback(translatable("plymouth.locking.allowed", toText(players), translatable(source.getWorld().getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
        return players.size();
    }

    private static int removePlayers(ServerCommandSource source, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayerOrThrow().interactionManager;
        iim.setManager(new RemovePlayersInteractionManager(iim, source, players));
        source.sendFeedback(translatable("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayers(ServerCommandSource source, BlockPos pos, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
        getPermissionHandlerIfAllowedModifyPermissions(source, pos, true).removePlayers(players);
        source.sendFeedback(translatable("plymouth.locking.removed", toText(players), translatable(source.getWorld().getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
        return players.size();
    }

    private static int setPermissions(ServerCommandSource source, int permissions) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayerOrThrow().interactionManager;
        iim.setManager(new ModifyPermissionsInteractionManager(iim, source, permissions));
        source.sendFeedback(translatable("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPermissions(ServerCommandSource source, BlockPos pos, int permissions) throws CommandSyntaxException {
        getPermissionHandlerIfAllowedModifyPermissions(source, pos, false).modifyPermissions(permissions);
        return Command.SINGLE_SUCCESS;
    }

    private static int getLock(ServerCommandSource source) throws CommandSyntaxException {
        var iim = (InjectableInteractionManager) source.getPlayerOrThrow().interactionManager;
        iim.setManager(new GetLockInteractionManager(iim, source));
        source.sendFeedback(translatable("commands.plymouth.locking.prompt"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getLock(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
        var handler = getBlockEntity(source.getWorld(), pos).plymouth$getPermissionHandler();
        if (handler != null) {
            handler.dumpLock(source);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendError(Text.literal("The selected block is not owned by anyone."));
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
                getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, true).removePlayers(players);
                source.sendFeedback(translatable("plymouth.locking.removed", toText(players), translatable(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text text) {
                    source.sendError(text);
                } else {
                    source.sendError(Text.literal(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }

    private static class AddPlayersInteractionManager extends InteractionManager {
        private final Collection<ServerPlayerEntity> players;
        private final short permissions;

        private AddPlayersInteractionManager(InjectableInteractionManager manager, ServerCommandSource source, Collection<ServerPlayerEntity> players, short permissions) {
            super(manager, source);
            this.players = players;
            this.permissions = permissions;
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, true).modifyPlayers(players, permissions);
                source.sendFeedback(translatable("plymouth.locking.allowed", toText(players), translatable(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text text) {
                    source.sendError(text);
                } else {
                    source.sendError(Text.literal(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }

    private static class ModifyPermissionsInteractionManager extends InteractionManager {
        private final int permissions;

        private ModifyPermissionsInteractionManager(InjectableInteractionManager manager, ServerCommandSource source, int permissions) {
            super(manager, source);
            this.permissions = permissions;
        }

        protected void tryInteraction(ServerWorld world, BlockPos pos) {
            try {
                getPermissionHandlerIfAllowedModifyPermissions(source, world, pos, false).modifyPermissions(permissions);
                source.sendFeedback(translatable("plymouth.locking.modified", "~", translatable(world.getBlockState(pos).getBlock().getTranslationKey()), toText(pos)), false);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text text) {
                    source.sendError(text);
                } else {
                    source.sendError(Text.literal(msg.getString()));
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
                    throw BLOCK_ENTITY_NOT_OWNED.create(translatable(world.getBlockState(pos).getBlock().getTranslationKey()));
                handler.dumpLock(source);
            } catch (CommandSyntaxException cse) {
                var msg = cse.getRawMessage();
                if (msg instanceof Text text) {
                    source.sendError(text);
                } else {
                    source.sendError(Text.literal(msg.getString()));
                }
            }
            manager.setManager(null);
        }
    }
}
