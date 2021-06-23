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
    BLOCK,
    /**
     * Indicates that the class is a {@link DeathRecord}.
     */
    DEATH,
    /**
     * Indicates that the class is a {@link InventoryRecord}.
     */
    INVENTORY,
    /**
     * Indicates that the class is a {@link LookupRecord}.
     */
    @Deprecated(forRemoval = true)
    LOOKUP,
    LOOKUP_BLOCK,
    LOOKUP_DEATH,
    LOOKUP_INVENTORY
}
