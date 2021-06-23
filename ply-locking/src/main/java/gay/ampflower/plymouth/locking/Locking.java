package gay.ampflower.plymouth.locking;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import gay.ampflower.plymouth.common.UUIDHelper;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
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

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public class Locking implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("Plymouth: Locking");

    public static final Predicate<ServerCommandSource>
            // LOCKING_BYPASS_PERMISSION = Permissions.require("plymouth.locking.bypass", 2),
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
            var face = state.get(ChestBlock.FACING);

            if ((otherPos = type == ChestType.SINGLE ? null : pos.offset(type == ChestType.LEFT ? face.rotateYClockwise() : face.rotateYCounterclockwise())) != null &&
                    ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                            otherState.get(ChestBlock.FACING) != face ||
                            otherState.get(ChestBlock.CHEST_TYPE) != type.getOpposite())) {
                otherPos = null;
            }
        } else if (block instanceof BedBlock) {
            var half = state.get(BedBlock.PART);
            var face = state.get(BedBlock.FACING);
            otherPos = pos.offset(half == BedPart.FOOT ? face : face.getOpposite());

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block ||
                    otherState.get(BedBlock.FACING) != face ||
                    otherState.get(BedBlock.PART) == half) {
                otherPos = null;
            }
        } else if (block instanceof DoorBlock) {
            var half = state.get(DoorBlock.HALF);
            otherPos = pos.offset(half == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);

            if ((otherState = world.getBlockState(otherPos)).getBlock() != block || otherState.get(DoorBlock.HALF) == half) {
                otherPos = null;
            }
        }
        return otherPos;
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
