package net.kjp12.plymouth.database;// Created 2021-08-04T00:37

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * What actions were taken on the block?
 *
 * @author KJP12
 * @since ${version}
 * @deprecated Will be replaced with old->new when possible.
 **/
@Deprecated
public enum BlockAction {
    BREAK(TextHelper.translatable("plymouth.tracker.action.broke").formatted(Formatting.RED)),
    PLACE(TextHelper.translatable("plymouth.tracker.action.placed").formatted(Formatting.GREEN)),
    USE(TextHelper.translatable("plymouth.tracker.action.used").formatted(Formatting.AQUA));

    public final Text niceName;

    BlockAction(Text niceName) {
        this.niceName = niceName;
    }
}
