package net.kjp12.helium;

import net.kjp12.helium.helpers.IShadowBlockEntity;
import net.kjp12.helium.helpers.db.NoopPlymouth;
import net.kjp12.helium.helpers.db.Plymouth;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * This class is a late-init-type class. It's expecting that
 * by the time that it's called, all blocks would have been
 * registered.
 */
public class Helium {
    public static final Logger LOGGER = LogManager.getLogger("Helium");
    public static final VoxelShape
            NORTH_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.NORTH),
            SOUTH_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.SOUTH),
            EAST_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.EAST),
            WEST_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.WEST),
            UP_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.UP),
            DOWN_FACE = VoxelShapes.extrudeFace(VoxelShapes.fullCube(), Direction.DOWN);
    /**
     * Anonymous Header Hashes, used for generating a UUID for blocks and entities
     * to interact with locked blocks. Most will be cached here, but there will be
     * some that need to be generated on the fly, ie. new blocks or entities.
     * <p>
     * If a new block or entity does try to modify a block, it will have full access
     * due to no permission checks existing for the new code. Beware of this.
     */
    public static final int
            HELIUM_HASH = "Helium".hashCode(),
            ENTITY_HASH = "Entity".hashCode(),
            BLOCK_HASH = "Block".hashCode();
    public static final long
            ENTITY_HEADER = ((long) HELIUM_HASH) << 32L | ((long) ENTITY_HASH),
            BLOCK_HEADER = ((long) HELIUM_HASH) << 32L | ((long) BLOCK_HASH);
    public static final UUID
            ANONYMOUS_UUID = Util.NIL_UUID,
            HOPPER_UUID = new UUID(BLOCK_HEADER, "TheHopper".hashCode()),
            TNT_UUID = new UUID(ENTITY_HEADER, "ThePrimedTNT".hashCode()),
            CREEPER_UUID = new UUID(ENTITY_HEADER, "TheCreeper".hashCode()),
            DRAGON_UUID = new UUID(ENTITY_HEADER, "TheEnderDragon".hashCode()),
            WITHER_UUID = new UUID(ENTITY_HEADER, "TheWither".hashCode());

    public static final Tag<Block>
            HIDDEN_BLOCKS = BlockTags.getContainer().get(new Identifier("helium", "hidden")),
            NO_SMEAR_BLOCKS = BlockTags.getContainer().get(new Identifier("helium", "no_smear"));
    public static Plymouth database = new NoopPlymouth();

    public static int wrapIndex(int index, int limit) {
        return index < 0 ? (index % limit) + limit : index % limit;
    }

    public static UUID getUUID(Entity breaker) {
        if (breaker == null) {
            return ANONYMOUS_UUID;
        } else if (breaker instanceof TntEntity) {
            var cause = ((TntEntity) breaker).getCausingEntity();
            return cause == null ? TNT_UUID : getUUID(cause);
        } else if (breaker instanceof CreeperEntity) {
            return CREEPER_UUID;
        } else if (breaker instanceof WitherEntity) {
            return WITHER_UUID;
        } else if (breaker instanceof EnderDragonEntity) {
            return DRAGON_UUID;
        } else return breaker.getUuid();
    }

    /**
     * Protection check blocks. If the cause is a non-player entity,
     * it'll attempt to get the cause of player's UUID or use the
     * anonymous hashes.
     *
     * @return true if the entity can break the block.
     */
    public static boolean canBreak(IShadowBlockEntity block, Entity breaker) {
        return block == null || (breaker instanceof PlayerEntity ? block.helium$canBreakBlock((PlayerEntity) breaker) : block.helium$canBreakBlock(getUUID(breaker)));
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
}
