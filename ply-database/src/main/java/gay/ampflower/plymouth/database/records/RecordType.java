package gay.ampflower.plymouth.database.records;

/**
 * The type of record. Used for switches and casting.
 *
 * @author Ampflower
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
