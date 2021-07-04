package net.kjp12.plymouth.locking;// Created 2021-03-22T22:16:29

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kjp12.plymouth.common.UUIDHelper;
import net.kjp12.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ToolItem;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * @author KJP12
 * @since 0.0.0
 */
public class Locking implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("Plymouth: Locking");

    public static final int
            OWNED = 512,
            OWNER = 256,
            READ_PERMISSION = 8,
            WRITE_PERMISSION = 4,
            DELETE_PERMISSION = 2,
            PERMISSIONS_PERMISSION = 1,
            READ_BYPASS = READ_PERMISSION << 4,
            WRITE_BYPASS = WRITE_PERMISSION << 4,
            DELETE_BYPASS = DELETE_PERMISSION << 4,
            PERMISSIONS_BYPASS = PERMISSIONS_PERMISSION << 4,
            FULL_PERMISSION = 15,
            DEFAULT_UMASK = 0x0FC0;

    public static final Predicate<ServerCommandSource>
            LOCKING_BYPASS_READ_PERMISSION = Permissions.require("plymouth.locking.bypass.read", 2),
            LOCKING_BYPASS_WRITE_PERMISSION = Permissions.require("plymouth.locking.bypass.write", 2),
            LOCKING_BYPASS_DELETE_PERMISSION = Permissions.require("plymouth.locking.bypass.delete", 2),
            LOCKING_BYPASS_PERMISSIONS_PERMISSION = Permissions.require("plymouth.locking.bypass.permissions", 2),
            LOCKING_LOCK_PERMISSION = Permissions.require("plymouth.locking.lock", true);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(LockCommand::register);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Runs permissions checks using MinecraftServer as the command source, preinitializing any permission nodes.
            // This will become redundant as command equivalents get implemented.
            var source = server.getCommandSource();
            LOCKING_BYPASS_READ_PERMISSION.test(source);
            LOCKING_BYPASS_WRITE_PERMISSION.test(source);
            LOCKING_BYPASS_DELETE_PERMISSION.test(source);
            LOCKING_BYPASS_PERMISSIONS_PERMISSION.test(source);
        });
    }

    public static boolean canReach(ServerPlayerEntity runner, BlockPos target) {
        return runner.squaredDistanceTo(Vec3d.ofCenter(target)) < 25;
    }

    /**
     * Protection check blocks. If the cause is a non-player entity,
     * it'll attempt to get the cause of player's UUID or use the
     * anonymous hashes.
     *
     * @return true if the entity can break the block.
     */
    public static boolean canBreak(ILockable block, Entity breaker) {
        return block == null || !block.plymouth$isOwned() || canBreak0(block.plymouth$getPermissionHandler(), breaker);
    }

    public static boolean canBreak(ILockable block, DamageSource breaker) {
        return block == null || !block.plymouth$isOwned() || canBreak0(block.plymouth$getPermissionHandler(), breaker);
    }

    public static boolean canBreak(ILockable block, Explosion breaker) {
        if (block == null || !block.plymouth$isOwned()) return true;
        var ph = block.plymouth$getPermissionHandler();
        var ce = breaker.getCausingEntity();
        if (ce != null) return canBreak0(ph, ce);
        var ds = breaker.getDamageSource();
        if (ds != null) return canBreak0(ph, ds);
        return ph.allowDelete(UUIDHelper.ANONYMOUS_UUID);
    }

    public static boolean canBreak0(IPermissionHandler block, DamageSource breaker) {
        var attackingEntity = breaker.getAttacker();
        if (attackingEntity != null) return canBreak0(block, attackingEntity);
        var sourceEntity = breaker.getSource();
        return sourceEntity != null ? canBreak0(block, sourceEntity) : block.allowDelete(UUIDHelper.getUUID(breaker));
    }

    public static boolean canBreak0(IPermissionHandler block, Entity breaker) {
        return breaker instanceof PlayerEntity ? block.allowDelete(breaker.getCommandSource()) : block.allowDelete(breaker);
    }

    public static boolean isItemNoop(ServerPlayerEntity entity, ItemStack stack) {
        if (stack.isEmpty()) return true;
        var item = stack.getItem();
        return (item.getClass() == Item.class && !item.isFood()) ||
                // Allow tool items, only if the player isn't being attacked.
                ((item instanceof ToolItem || item instanceof ShieldItem) && entity.getAttacker() == null) ||
                !entity.getHungerManager().isNotFull();
    }

    public static LockDelegate surrogate(World world, BlockPos pos, ServerCommandSource source) {
        var b = (ILockable) world.getBlockEntity(pos);
        if (b == null) return new LockDelegate(FULL_PERMISSION, Collections.emptyList());
        var h = b.plymouth$getPermissionHandler();
        var ep = h == null ? 0 : h.effectivePermissions(source) | OWNED;
        var list = new ArrayList<ILockable>();
        list.add(b);
        var p2 = getOtherPos(world, pos);
        if (p2 != null) {
            if ((b = (ILockable) world.getBlockEntity(p2)) != null) {
                if ((h = b.plymouth$getPermissionHandler()) != null) ep |= h.effectivePermissions(source);
                list.add(b);
            }
        }
        return new LockDelegate(ep == 0 ? FULL_PERMISSION : ep, list);
    }

    // We want to effect both blocks if at all possible.
    // Note: CHEST and TRAPPED_CHEST are not compatible and should be treated as two separate entities.
    //  This can be seen in game by setting one to LEFT and the other to RIGHT and trying to open one of them.
    //  We may ignore this as long the other block we match against is of a fitting state.
    public static BlockPos getOtherPos(World world, BlockPos pos) {
        BlockPos otherPos = null;
        BlockState state = world.getBlockState(pos), otherState;
        var block = state.getBlock();
        if (block instanceof ChestBlock) {
            var type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE) return null;
            var face = state.get(ChestBlock.FACING);

            otherPos = pos.offset(type == ChestType.LEFT ? face.rotateYClockwise() : face.rotateYCounterclockwise());
            if ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                    otherState.get(ChestBlock.FACING) != face ||
                    otherState.get(ChestBlock.CHEST_TYPE) != type.getOpposite()) {
                return null;
            }
        } else if (block instanceof BedBlock) {
            var half = state.get(BedBlock.PART);
            var face = state.get(BedBlock.FACING);
            otherPos = pos.offset(half == BedPart.FOOT ? face : face.getOpposite());

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                    otherState.get(BedBlock.FACING) != face ||
                    otherState.get(BedBlock.PART) == half) {
                return null;
            }
        } else if (block instanceof DoorBlock) {
            var half = state.get(DoorBlock.HALF);
            otherPos = pos.offset(half == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block || otherState.get(DoorBlock.HALF) == half) {
                return null;
            }
        }
        return otherPos;
    }

    private static final int A = 0, O = 16, G = 32, U = O | G, RA = 0, RM = 64, RE = 128, R = RM | RE;

    /**
     * <ul>
     *     <li><code>u</code> - user</li>
     *     <li><code>g</code> - group</li>
     *     <li><code>o</code> - other</li>
     *     <li><code>a</code> - all, default</li>
     * </ul>
     * <ul>
     *     <li><code>r</code> - read</li>
     *     <li><code>w</code> - write</li>
     *     <li><code>d</code> - delete</li>
     *     <li><code>p</code> - modify permissions</li>
     * </ul>
     */
    public static int fromString(String str) {
        int state = A, ret = 0;
        for (int i = 0, l = str.length(); i < l; i++) {
            char c = str.charAt(i);
            switch (c) {
                case ' ', ',' -> {
                }

                case 'r' -> state |= READ_PERMISSION;
                case 'w' -> state |= WRITE_PERMISSION;
                case 'd' -> state |= DELETE_PERMISSION;
                case 'p' -> state |= PERMISSIONS_PERMISSION;

                case '-' -> {
                    ret = computeReturn(state, ret);
                    state = (state & ~R) | RM;
                }
                case '=' -> {
                    ret = computeReturn(state, ret);
                    state = (state & ~R) | RE;
                }
                case '+' -> {
                    ret = computeReturn(state, ret);
                    state &= ~R;
                }

                case 'o' -> {
                    ret = computeReturn(state, ret);
                    state = O;
                }
                case 'g' -> {
                    ret = computeReturn(state, ret);
                    state = G;
                }
                case 'u' -> {
                    ret = computeReturn(state, ret);
                    state = U;
                }
                case 'a' -> {
                    ret = computeReturn(state, ret);
                    state = A;
                }

                default -> throw new IllegalArgumentException("Unexpected value " + c + " at " + i + " in " + str);
            }
        }
        return computeReturn(state, ret);
    }

    private static int computeReturn(int state, int ret) {
        if (state == 0) return ret;
        int m = state & FULL_PERMISSION, u;
        switch (state & U) {
            case A -> {
                m |= (m << 4) | (m << 8);
                u = 0x0FFF;
            }
            case U -> {
                m <<= 8;
                u = 0x0F00;
            }
            case G -> {
                m <<= 4;
                u = 0x00F0;
            }
            case O -> u = 0x000F;
            default -> throw new AssertionError("I think your JVM broke.");
        }
        return switch (state & R) {
            case RM -> (ret & ~m) | m << 16;
            case RE -> (ret & ~u) | m | u << 16;
            case RA -> (ret & ~(m << 16)) | m;
            default -> throw new IllegalArgumentException("invalid state");
        };
    }

    public static String toString(byte permissions) {
        char[] c = "----".toCharArray();
        if ((permissions & READ_PERMISSION) != 0) c[0] = 'r';
        if ((permissions & WRITE_PERMISSION) != 0) c[1] = 'w';
        if ((permissions & DELETE_PERMISSION) != 0) c[2] = 'd';
        if ((permissions & PERMISSIONS_PERMISSION) != 0) c[3] = 'p';
        return String.valueOf(c);
    }

    public static String toString(int permissions) {
        char[] c = "------------".toCharArray();
        for (int i = 0; i < 3; i++) {
            int s = (2 - i) * 4, m = i * 4;
            if ((permissions & (READ_PERMISSION << s)) != 0) c[m] = 'r';
            if ((permissions & (WRITE_PERMISSION << s)) != 0) c[m + 1] = 'w';
            if ((permissions & (DELETE_PERMISSION << s)) != 0) c[m + 2] = 'd';
            if ((permissions & (PERMISSIONS_PERMISSION << s)) != 0) c[m + 3] = 'p';
        }
        return String.valueOf(c);
    }

    public static Text toText(Block block) {
        return new TranslatableText(block.getTranslationKey()).formatted(Formatting.AQUA);
    }

    public static Text toText(Collection<? extends Nameable> nameables) {
        if (nameables == null || nameables.isEmpty()) {
            return new LiteralText("?");
        }
        var itr = nameables.iterator();
        var base = new LiteralText("");
        base.append(itr.next().getDisplayName());
        while (itr.hasNext()) {
            base.append(Texts.GRAY_DEFAULT_SEPARATOR_TEXT).append(itr.next().getDisplayName());
        }
        return base;
    }

    public static Text toText(BlockPos pos) {
        return new TranslatableText("chat.coordinates", pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA);
    }
}
