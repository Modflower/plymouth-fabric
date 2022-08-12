package gay.ampflower.plymouth.database;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * What actions were taken on the block?
 *
 * @author Ampflower
 * @since ${version}
 * @deprecated Will be replaced with old->new when possible.
 **/
@Deprecated
public enum BlockAction {
    BREAK(Text.translatable("plymouth.tracker.action.broke").formatted(Formatting.RED)),
    PLACE(Text.translatable("plymouth.tracker.action.placed").formatted(Formatting.GREEN)),
    USE(Text.translatable("plymouth.tracker.action.used").formatted(Formatting.AQUA));

    public final Text niceName;

    BlockAction(Text niceName) {
        this.niceName = niceName;
    }
}
