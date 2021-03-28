package net.kjp12.helium;

import com.mojang.datafixers.DataFixer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.chunk.IdListPalette;
import net.minecraft.world.chunk.Palette;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.http.HttpClient;

/**
 * This class is a late-init-type class. It's expecting that
 * by the time that it's called, all blocks would have been
 * registered.
 */
public class Helium {
    public static final HttpClient httpClient = HttpClient.newHttpClient();
    public static final Text
            SEE_LOGS = new LiteralText("See the server logs for more information.").formatted(Formatting.ITALIC);
    public static final Palette<BlockState> BLOCK_STATE_PALETTE = new IdListPalette<>(Block.STATE_IDS, Blocks.AIR.getDefaultState());

    public static int toChunkIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    /**
     * [vanilla-copy] {@link net.minecraft.world.PersistentStateManager#readTag(String, int)}
     *
     * @param stream      Stream to read NBT data from.
     * @param dfu         Server's DataFixer
     * @param dataVersion The data version to update to.
     * @return The NBT data, datafixed if necessary.
     */
    public static CompoundTag readTag(InputStream stream, DataFixer dfu, int dataVersion) throws IOException {
        try (var inputStream = stream; var pushback = new PushbackInputStream(inputStream, 2)) {
            CompoundTag tag;
            if (isCompressed(pushback)) {
                tag = NbtIo.readCompressed(pushback);
            } else {
                try (var dataInput = new DataInputStream(pushback)) {
                    tag = NbtIo.read(dataInput);
                }
            }
            int i = tag.contains("DataVersion", 99) ? tag.getInt("DataVersion") : 1343;
            return NbtHelper.update(dfu, DataFixTypes.SAVED_DATA, tag, i, dataVersion);
        }
    }

    /**
     * [vanilla-copy] {@link net.minecraft.world.PersistentStateManager#isCompressed(PushbackInputStream)}
     */
    public static boolean isCompressed(PushbackInputStream pushback) throws IOException {
        byte[] bytes = new byte[2];
        int i;
        boolean r = ((i = pushback.read(bytes, 0, 2)) == 2 && ((bytes[1] & 255) << 8 | bytes[0] & 255) == 35615);
        if (i != 0) pushback.unread(bytes, 0, i);
        return r;
    }
}
