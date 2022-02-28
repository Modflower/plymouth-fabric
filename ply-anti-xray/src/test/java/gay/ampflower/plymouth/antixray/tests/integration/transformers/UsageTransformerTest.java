package gay.ampflower.plymouth.antixray.tests.integration.transformers;

import gay.ampflower.plymouth.antixray.Constants;
import gay.ampflower.plymouth.antixray.ShadowChunk;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * @author Ampflower
 * @see gay.ampflower.plymouth.antixray.transformers.GudAsmTransformer
 * @see net.minecraft.server.world.ChunkHolder
 * @since ${version}
 **/
public class UsageTransformerTest {
    public static void simpleDirectSingleBlockUsage(WorldChunk chunk, BlockPos pos) {
        var block = chunk.getBlockState(pos);
        sink(new BlockUpdateS2CPacket(pos, block), chunk, "Direct <init>(BlockPos, BlockState) failure");
        intendedSideEffect(chunk.getWorld(), block, pos);
        unintendedSideEffect(chunk.getWorld(), block, pos);
        var packet = new BlockUpdateS2CPacket(chunk, pos);
        sink(packet, chunk, "Direct <init>(BlockView, BlockPos) failure");
        intendedSideEffect(chunk.getWorld(), packet.getState(), packet.getPos());
    }

    public static void simpleIfDirectSingleBlockUsage(WorldChunk chunk, BlockPos pos, boolean potato) {
        var packet = new BlockUpdateS2CPacket(pos, potato ? chunk.getBlockState(pos) : Blocks.AIR.getDefaultState());
        sink(packet, chunk, "Direct <init>(BlockPos, boolean ? BlockState : AIR) failure");
        intendedSideEffect(chunk.getWorld(), packet.getState(), packet.getPos());
    }

    /**
     * Worst case scenario method for the transformer... and various decompilers.
     */
    public static void acidTest(WorldChunk chunk, BlockPos pos, AcidTest acid) {
        {
            var packet = new BlockUpdateS2CPacket(pos, chunk.isOutOfHeightLimit(pos) ? Blocks.VOID_AIR.getDefaultState() : chunk.getBlockState(pos));
            sink(packet, chunk, "ACID 1");
            intendedSideEffect(chunk.getWorld(), packet.getState(), packet.getPos());
        }
        {
            boolean spuriousFailure = Math.random() > 0.15D;
            var packet = new BlockUpdateS2CPacket(pos, spuriousFailure ? chunk.getBlockState(pos) : Blocks.VOID_AIR.getDefaultState());
            sink(packet, chunk, "ACID 2");
        }
        {
            var block = chunk.getBlockState(pos);
            var packet = new BlockUpdateS2CPacket(pos, block);
            sink(packet, chunk, "ACID 3");
        }
        {
            BlockState a = Blocks.VOID_AIR.getDefaultState(), b = Blocks.VOID_AIR.getDefaultState();
            var world = chunk.getWorld();
            var acid1 = acid == AcidTest.ACID_1;
            var block = acid1 ? (a = chunk.getBlockState(pos)) : (b = world.getBlockState(pos));
            var packet = new BlockUpdateS2CPacket(pos, block);
            sink(packet, chunk, "ACID 4");
            intendedSideEffect(world, acid1 ? b : a, pos);
            unintendedSideEffect(world, acid1 ? a : b, pos);
        }
    }

    public static void intendedSideEffect(World world, BlockState block, BlockPos pos) {
        if (block.hasBlockEntity()) {
            assert !block.isIn(Constants.HIDDEN_BLOCKS) || !((ShadowChunk) world.getChunk(pos)).plymouth$isMasked(pos) :
                    "Intended side effect wrongfully kept.";
            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                world.sendPacket(blockEntity.toUpdatePacket());
            }
        }
    }

    public static void unintendedSideEffect(World world, BlockState block, BlockPos pos) {
        var chunk = (WorldChunk & ShadowChunk) world.getChunk(pos);
        assert chunk.getBlockState(pos) == block : "Unintended side effect wrongfully mangled.";
        world.setBlockState(pos, block);
    }

    public static void sink(BlockUpdateS2CPacket packet, WorldChunk origin, String message) {
        boolean a = sinkA(packet, origin), b = sinkB(packet.getState(), packet.getPos(), origin);
        assert a == b : message + ": Sinks disagree, transformer mangled.";
        assert !a : message + ": Sinks mismatched, transformer failed.";
    }

    public static boolean sinkA(BlockUpdateS2CPacket packet, WorldChunk origin) {
        var chunk = (WorldChunk & ShadowChunk) origin;
        return chunk.plymouth$isMasked(packet.getPos()) ^ (chunk.getBlockState(packet.getPos()) == packet.getState());
    }

    public static boolean sinkB(BlockState state, BlockPos pos, WorldChunk origin) {
        var chunk = (WorldChunk & ShadowChunk) origin;
        return chunk.plymouth$isMasked(pos) ^ (chunk.getBlockState(pos) == state);
    }

    private enum AcidTest {
        ACID_1, ACID_2, ACID_3
    }
}
