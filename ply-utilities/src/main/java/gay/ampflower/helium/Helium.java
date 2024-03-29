package gay.ampflower.helium;

import com.mojang.datafixers.DataFixer;
import net.minecraft.block.Blocks;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import static net.minecraft.text.Text.*;
import static net.minecraft.util.Formatting.*;

/**
 * This class is a late-init-type class. It's expecting that
 * by the time that it's called, all blocks would have been
 * registered.
 *
 * @author Ampflower
 * @since ${version}
 */
public class Helium {
    public static final Style LINK = Style.EMPTY.withFormatting(AQUA, UNDERLINE);
    public static final Text
            SEE_LOGS = literal("See the server logs for more information.").formatted(ITALIC),
            DID_YOU_MEAN = translatable("plymouth.dym", keybind("key.inventory").formatted(AQUA))
                    .formatted(ITALIC, RED),
            ENDER_CHEST = translatable(Blocks.ENDER_CHEST.getTranslationKey()).formatted(DARK_PURPLE);
    public static final Logger logger = LoggerFactory.getLogger("Plymouth");

    /**
     * [vanilla-copy] {@link net.minecraft.world.PersistentStateManager#readNbt(String, int)}
     *
     * @param stream      Stream to read NBT data from.
     * @param dfu         Server's DataFixer
     * @param dataVersion The data version to update to.
     * @return The NBT data, datafixed if necessary.
     */
    public static NbtCompound readTag(InputStream stream, DataFixer dfu, int dataVersion) throws IOException {
        try (var inputStream = stream; var pushback = new PushbackInputStream(inputStream, 2)) {
            NbtCompound tag;
            if (isCompressed(pushback)) {
                tag = NbtIo.readCompressed(pushback);
            } else {
                try (var dataInput = new DataInputStream(pushback)) {
                    tag = NbtIo.read(dataInput);
                }
            }
            int i = tag.contains("DataVersion", 99) ? tag.getInt("DataVersion") : 1343;
            return DataFixTypes.SAVED_DATA.update(dfu, tag, i, dataVersion);
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
