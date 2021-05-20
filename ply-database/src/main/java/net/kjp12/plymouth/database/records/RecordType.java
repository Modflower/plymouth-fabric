package net.kjp12.plymouth.database.records;// Created 2021-01-05T17:22:45

/**
 * The type of record. Used for switches and casting.
 *
 * @author KJP12
 * @since ${version}
 **/
public enum RecordType {
    /**
     * Indicates that the class is a {@link BlockRecord}.
     */
    BLOCK(LookupRecord.MOD_BLOCK),
    /**
     * Indicates that the class is a {@link DeathRecord}.
     */
    DEATH(LookupRecord.MOD_DEATH),
    /**
     * Indicates that the class is a {@link InventoryRecord}.
     */
    INVENTORY(LookupRecord.MOD_INVEN),
    /**
     * Indicates that the class is a {@link LookupRecord}.
     */
    LOOKUP(-1),
    ;
    public final int bits;

    RecordType(int bits) {
        this.bits = bits;
    }
}
