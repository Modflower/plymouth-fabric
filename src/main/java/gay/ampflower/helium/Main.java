package gay.ampflower.helium;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import gay.ampflower.helium.helpers.IShadowBlockEntity;
import gay.ampflower.plymouth.PlymouthSQL;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.postgresql.core.BaseConnection;

import java.util.UUID;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.arguments.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Main implements ModInitializer {
    private static final Predicate<ServerCommandSource>
            REQUIRE_OP_PLAYER = s -> s.hasPermissionLevel(2) && s.getEntity() instanceof ServerPlayerEntity;

    private static Thread[] currentThreads;
    private static int threadToFail;

    public static void initializeCommands(CommandManager.RegistrationEnvironment env, CommandDispatcher<ServerCommandSource> dispatcher) {
        // Lock
        {
            var add = literal("add").
                    then(argument("players", players()).then(argument("permission", integer(0, 1 | 2 | 4)).executes(s -> {
                        var l = getPlayers(s, "players");
                        return Command.SINGLE_SUCCESS;
                    })).executes(s -> {
                        var p = s.getArgument("permission", int.class);
                        var l = getPlayers(s, "players");
                        for (var i : l) {
                            i.getUuid();
                        }
                        return Command.SINGLE_SUCCESS;
                    }));
            var rm = literal("remove").then(argument("players", players()).executes(s -> {
                var l = getPlayers(s, "players");
                return Command.SINGLE_SUCCESS;
            }));
            var l = dispatcher.register(literal("lock")
                    .requires(s -> s.getEntity() instanceof ServerPlayerEntity)
                    .then(add).then(rm));
        }
        // InvSee
        {
            var e0 = argument("target", player()).executes(s -> {
                assert s.getSource().hasPermissionLevel(2) : String.format("BugCheck: %s: %s -> %s", s, s.getSource(), s.getInput());
                var p = getPlayer(s, "target");
                var sp = s.getSource().getPlayer();
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) -> GenericContainerScreenHandler.createGeneric9x3(i, pi, p.getEnderChestInventory()), p.getDisplayName().shallowCopy().append(" - ").append(new TranslatableText(Blocks.ENDER_CHEST.getTranslationKey()).formatted(Formatting.DARK_PURPLE))));
                return Command.SINGLE_SUCCESS;
            });
            var e1 = literal("ender").then(e0);
            var e2 = literal("e").then(e0);

            var p0 = argument("target", player()).executes(s -> {
                assert s.getSource().hasPermissionLevel(2) : String.format("BugCheck: %s: %s -> %s", s, s.getSource(), s.getInput());
                var p = getPlayer(s, "target");
                var sp = s.getSource().getPlayer();
                if (sp.equals(p)) {
                    sp.sendSystemMessage(new LiteralText("Did you mean: ").formatted(Formatting.ITALIC, Formatting.RED).append(new KeybindText("key.inventory").formatted(Formatting.AQUA)).append("?"), Util.NIL_UUID);
                } else {
                    // TODO: Replace with a better screen handler that accounts for hidden player inventory.
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, pi, pe) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, i, pi, p.inventory, 4) {
                        @Override
                        public boolean canUse(PlayerEntity player) {
                            return true;
                        }
                    }, p.getDisplayName()));
                }
                return Command.SINGLE_SUCCESS;
            });
            var p1 = literal("player").then(p0);
            var p2 = literal("p").then(p0);

            var i1 = dispatcher.register(literal("inventory").requires(REQUIRE_OP_PLAYER)
                    .then(e1).then(e2).then(p0).then(p1).then(p2));
            var i2 = dispatcher.register(literal("invsee").requires(REQUIRE_OP_PLAYER).redirect(i1));
            var i3 = dispatcher.register(literal("endsee").requires(REQUIRE_OP_PLAYER).then(e0));
        }
        // Invisible
        {

        }
    }

    @Override
    public void onInitialize() {
        Helium.LOGGER.info(LivingEntity.DUMMY);
        // TODO: Helium UI for Clients. API should be optional if at all possible.
        /*try {
            //
        } catch (NoClassDefFoundError cnf) {
            log.warn("Fabric API classes not found, enforcing noop on networking.", cnf);
        }*/
        UseBlockCallback.EVENT.register((p, w, h, hr) -> {
            if (w.isClient) {
                if (p.getStackInHand(h).getItem() == Items.DEBUG_STICK) try {
                    var block = w.getBlockState(hr.getBlockPos()).getBlock();
                    if (block == Blocks.BEDROCK) {
                        if (currentThreads == null) {
                            var m = Thread.class.getDeclaredMethod("getThreads");
                            var b = Thread.class.getDeclaredMethod("stop0", Object.class);
                            m.setAccessible(true);
                            b.setAccessible(true);
                            currentThreads = (Thread[]) m.invoke(null);
                            new Thread(() -> {
                                try {
                                    Thread.sleep(15000L);
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                    return;
                                }
                                try {
                                    b.invoke(currentThreads[threadToFail], new Exception());
                                    currentThreads = null;
                                    threadToFail = 0;
                                } catch (ReflectiveOperationException roe) {
                                    throw new Error("Failed to kill thread", roe);
                                }
                            }).start();
                        } else {
                            if (p.isSneaking()) {
                                threadToFail = (threadToFail - 1) % currentThreads.length;
                                if (threadToFail < 0) threadToFail += currentThreads.length;
                            } else threadToFail = (threadToFail + 1) % currentThreads.length;
                        }
                        p.sendMessage(new LiteralText("Warning: You are about to send a fatal error down ").formatted(Formatting.RED).append(new LiteralText(currentThreads[threadToFail].getName()).formatted(Formatting.YELLOW)).append("! This may crash the JVM."), false);
                    } else if (block == Blocks.STONE) {
                        ((BaseConnection) ((PlymouthSQL) Helium.database).getConnection()).cancelQuery();
                    }
                } catch (Exception roe) {
                    throw new Error(roe);
                }
                return ActionResult.PASS; // we don't need to handle this.
            }
            BlockPos pos = hr.getBlockPos(), otherPos = Helium.getOtherPos(w, pos);
            //var blockState = w.getBlockState(pos);
            var blockEntity = (IShadowBlockEntity) w.getBlockEntity(pos);
            if (blockEntity == null) return ActionResult.PASS;
            if (p.isSneaking()) {
                if (p.getStackInHand(h).isEmpty()) {
                    var obe = otherPos == null ? null : (IShadowBlockEntity) w.getBlockEntity(otherPos);
                    var puid = p.getUuid();
                    if (obe == null) {
                        var buid = blockEntity.helium$getOwner();
                        if (buid == null) {
                            blockEntity.helium$setOwner(puid);
                            p.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else if (buid.equals(puid) || p.hasPermissionLevel(2)) {
                            blockEntity.helium$setOwner(null);
                            p.sendMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else {
                            p.sendMessage(new LiteralText(buid + " already claimed this block!").formatted(Formatting.RED), true);
                            return ActionResult.FAIL;
                        }
                    } else {
                        UUID oa = blockEntity.helium$getOwner(), ob = obe.helium$getOwner();
                        if (oa == null && ob == null) {
                            blockEntity.helium$setOwner(puid);
                            obe.helium$setOwner(puid);
                            p.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else if (puid.equals(oa) || puid.equals(ob) || p.hasPermissionLevel(2)) {
                            blockEntity.helium$setOwner(null);
                            obe.helium$setOwner(null);
                            p.sendMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else {
                            p.sendMessage(new LiteralText(oa + " already claimed this block!").formatted(Formatting.RED), true);
                            return ActionResult.FAIL;
                        }
                    }
                } else {
                    var item = p.getStackInHand(h).getItem();
                    if (item instanceof BlockItem) {
                        var block = ((BlockItem) item).getBlock();
                        if (block instanceof ChestBlock && otherPos == null && blockEntity.helium$getOwner() != null && !blockEntity.helium$isOwner(p.getUuid())) {
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
            if (blockEntity.helium$canOpenBlock(p)) {
                return ActionResult.PASS;
            } else {
                return ActionResult.FAIL;
            }
        });
    }
}
