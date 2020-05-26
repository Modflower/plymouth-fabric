package net.kjp12.sodium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.kjp12.sodium.helpers.IProtectBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.BlockItem;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class Main implements ModInitializer {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((p, w, h, hr) -> {
            if(w.isClient) return ActionResult.PASS; // we don't need to handle this.
            BlockPos pos = hr.getBlockPos(), otherPos = getOtherPos(w, pos);
            var blockEntity = w.getBlockEntity(pos);
            if (blockEntity == null) return ActionResult.PASS;
            var ipd = (IProtectBlock) blockEntity;
            if (p.isSneaking()) {
                if(p.getStackInHand(h).isEmpty()) {
                    var obe = otherPos == null ? null : (IProtectBlock) w.getBlockEntity(otherPos);
                    var puid = p.getUuid();
                    if (obe == null) {
                        var buid = ipd.sodium$getOwner();
                        if (buid == null) {
                            ipd.sodium$setOwner(puid);
                            p.sendSystemMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), Util.field_25140);
                            return ActionResult.SUCCESS;
                        } else if (buid.equals(puid) || p.hasPermissionLevel(2)) {
                            ipd.sodium$setOwner(null);
                            p.sendSystemMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), Util.field_25140);
                            return ActionResult.SUCCESS;
                        } else {
                            p.sendSystemMessage(new LiteralText(buid + " already claimed this block!").formatted(Formatting.RED), Util.field_25140);
                            return ActionResult.FAIL;
                        }
                    } else {
                        UUID oa = ipd.sodium$getOwner(), ob = obe.sodium$getOwner();
                        if (oa == null && ob == null) {
                            ipd.sodium$setOwner(puid);
                            obe.sodium$setOwner(puid);
                            p.sendSystemMessage(new LiteralText("Successfully claimed block.").formatted(Formatting.GREEN), Util.field_25140);
                            return ActionResult.SUCCESS;
                        } else if (puid.equals(oa) || puid.equals(ob) || p.hasPermissionLevel(2)) {
                            ipd.sodium$setOwner(null);
                            obe.sodium$setOwner(null);
                            p.sendSystemMessage(new LiteralText("Successfully unclaimed block.").formatted(Formatting.GREEN), Util.field_25140);
                            return ActionResult.SUCCESS;
                        } else {
                            p.sendSystemMessage(new LiteralText(oa + " already claimed this block!").formatted(Formatting.RED), Util.field_25140);
                            return ActionResult.FAIL;
                        }
                    }
                } else {
                    var item = p.getStackInHand(h).getItem();
                    if(item instanceof BlockItem) {
                        var block = ((BlockItem) item).getBlock();
                        if(block instanceof ChestBlock && otherPos != null && !ipd.sodium$isOwner(p.getUuid())) {
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
            if (ipd.sodium$canOpenBlock(p)) {
                return ActionResult.PASS;
            } else {
                return ActionResult.FAIL;
            }
        });
        /*AttackBlockCallback.EVENT.register((p, w, h, hr) -> {
            ;
        });*/
    }

    // We want to effect both blocks if at all possible.
    // Note: CHEST and TRAPPED_CHEST are not compatible and should be treated as two separate entities.
    //  This can be seen in game by setting one to LEFT and the other to RIGHT and trying to open one of them.
    //  We may ignore this as long the other block we match against is of a fitting state.
    public static BlockPos getOtherPos(World world, BlockPos pos) {
        BlockPos otherPos = null;
        BlockState state = world.getBlockState(pos), otherState;
        if(state.getBlock() instanceof ChestBlock) {
            var type = state.get(ChestBlock.CHEST_TYPE);
            if(type == ChestType.LEFT) {
                switch(state.get(ChestBlock.FACING)) {
                    case NORTH: otherPos = pos.west(1); break;
                    case SOUTH: otherPos = pos.east(1); break;
                    case EAST: otherPos = pos.south(1); break;
                    case WEST: otherPos = pos.north(1); break;
                    default: System.out.printf("Illegal Chest state @ %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                }
                if(otherPos != null && world.getBlockState(otherPos).get(ChestBlock.CHEST_TYPE) != ChestType.RIGHT) {
                    otherPos = null;
                }
            } else if(type == ChestType.RIGHT) {
                switch(state.get(ChestBlock.FACING)) {
                    case NORTH: otherPos = pos.east(1); break;
                    case SOUTH: otherPos = pos.west(1); break;
                    case EAST: otherPos = pos.north(1); break;
                    case WEST: otherPos = pos.south(1); break;
                    default: System.out.printf("Illegal Chest state @ %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                }
                if(otherPos != null && world.getBlockState(otherPos).get(ChestBlock.CHEST_TYPE) != ChestType.LEFT) {
                    otherPos = null;
                }
            }
        } else if(state.getBlock() instanceof DoorBlock) {
            var half = state.get(DoorBlock.HALF);

            if(half == DoubleBlockHalf.UPPER && (!((otherState = world.getBlockState(otherPos = pos.down(1))).getBlock() instanceof DoorBlock) || otherState.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER)) {
                otherPos = null;
            }
        }
        return otherPos;
    }
}
