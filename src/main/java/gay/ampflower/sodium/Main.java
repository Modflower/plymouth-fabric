package gay.ampflower.sodium;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import gay.ampflower.sodium.helpers.IShadowBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.BlockItem;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class Main implements ModInitializer {
    public static final Logger log = LogManager.getLogger("Sodium");

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
            if (type == ChestType.LEFT) {
                switch (state.get(ChestBlock.FACING)) {
                    case NORTH:
                        otherPos = pos.east(1);
                        break;
                    case SOUTH:
                        otherPos = pos.west(1);
                        break;
                    case EAST:
                        otherPos = pos.south(1);
                        break;
                    case WEST:
                        otherPos = pos.north(1);
                        break;
                    default:
                        System.out.printf("Illegal Chest state @ %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                }
                if (otherPos != null && world.getBlockState(otherPos).get(ChestBlock.CHEST_TYPE) != ChestType.RIGHT) {
                    otherPos = null;
                }
            } else if (type == ChestType.RIGHT) {
                switch (state.get(ChestBlock.FACING)) {
                    case NORTH:
                        otherPos = pos.west(1);
                        break;
                    case SOUTH:
                        otherPos = pos.east(1);
                        break;
                    case EAST:
                        otherPos = pos.north(1);
                        break;
                    case WEST:
                        otherPos = pos.south(1);
                        break;
                    default:
                        System.out.printf("Illegal Chest state @ %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                }
                if (otherPos != null && world.getBlockState(otherPos).get(ChestBlock.CHEST_TYPE) != ChestType.LEFT) {
                    otherPos = null;
                }
            }
        } else if (block instanceof DoorBlock) {
            var half = state.get(DoorBlock.HALF);

            if (half == DoubleBlockHalf.UPPER && (!((otherState = world.getBlockState(otherPos = pos.down(1))).getBlock() instanceof DoorBlock) || otherState.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER)) {
                otherPos = null;
            }
        }
        return otherPos;
    }

    public static void initializeCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
    }

    @Override
    public void onInitialize() {
        // TODO: Sodium UI for Clients. API should be optional if at all possible.
        /*try {
            //
        } catch (NoClassDefFoundError cnf) {
            log.warn("Fabric API classes not found, enforcing noop on networking.", cnf);
        }*/
        // TODO: PostgreSQL Driver
        /*try {
            var driver = new Driver();
        } catch (NoClassDefFoundError cnf) {
            log.warn("PostgreSQL Driver not loaded, falling back to SQLite.", cnf);
        }*/
        UseBlockCallback.EVENT.register((p, w, h, hr) -> {
            if (w.isClient) return ActionResult.PASS; // we don't need to handle this.
            BlockPos pos = hr.getBlockPos(), otherPos = getOtherPos(w, pos);
            //var blockState = w.getBlockState(pos);
            var blockEntity = (IShadowBlockEntity) w.getBlockEntity(pos);
            if (blockEntity == null) return ActionResult.PASS;
            if (p.isSneaking()) {
                if (p.getStackInHand(h).isEmpty()) {
                    var obe = otherPos == null ? null : (IShadowBlockEntity) w.getBlockEntity(otherPos);
                    var puid = p.getUuid();
                    if (obe == null) {
                        var buid = blockEntity.sodium$getOwner();
                        if (buid == null) {
                            blockEntity.sodium$setOwner(puid);
                            p.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else if (buid.equals(puid) || p.hasPermissionLevel(2)) {
                            blockEntity.sodium$setOwner(null);
                            p.sendMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else {
                            p.sendMessage(new LiteralText(buid + " already claimed this block!").formatted(Formatting.RED), true);
                            return ActionResult.FAIL;
                        }
                    } else {
                        UUID oa = blockEntity.sodium$getOwner(), ob = obe.sodium$getOwner();
                        if (oa == null && ob == null) {
                            blockEntity.sodium$setOwner(puid);
                            obe.sodium$setOwner(puid);
                            p.sendMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), true);
                            return ActionResult.SUCCESS;
                        } else if (puid.equals(oa) || puid.equals(ob) || p.hasPermissionLevel(2)) {
                            blockEntity.sodium$setOwner(null);
                            obe.sodium$setOwner(null);
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
                        if (block instanceof ChestBlock && otherPos == null && blockEntity.sodium$getOwner() != null && !blockEntity.sodium$isOwner(p.getUuid())) {
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
            if (blockEntity.sodium$canOpenBlock(p)) {
                return ActionResult.PASS;
            } else {
                return ActionResult.FAIL;
            }
        });
    }
}
