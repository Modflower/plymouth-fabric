package net.kjp12.plymouth.database.records;// Created 2021-01-05T17:06:52

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

/**
 * Record interface. Only carries methods for flags and type.
 * <p>
 * Most records typically hold the cause and target, including world, pos and entity's name and UUID.
 *
 * @author Ampflower
 * @see BlockRecord
 * @see DeathRecord
 * @see InventoryRecord
 * @see LookupRecord
 * @see BlockLookupRecord
 * @see InventoryLookupRecord
 * @since ${version}
 **/
public interface PlymouthRecord {
    /**
     * Creates a formatted text object to display the time, cause, target and position of the record.
     *
     * @return Translatable text for use with rendering the logs and sending to the client.
     */
    @NotNull
    Text toText();

    /**
     * Creates a formatted text object to display the time, cause and target of the record.
     *
     * @return Translatable text for use with rendering the logs and sending to the client.
     */
    @NotNull
    default Text toTextNoPosition() {
        return toText();
    }

    /**
     * Bitwise flag of what the record is for.
     * Please see the individual classes for what the bitwise flags are for.
     *
     * @return flags if overridden, else 0 by default.
     * @see InventoryRecord#flags()
     * @see LookupRecord#flags()
     */
    // Please link any records that overrides flags here with `@see <Class>#flags()`.
    // A link to the field is sufficient should there only be a field implementing this.

    // The name is purposefully `flags` to allow a passive override by record.
    default int flags() {
        return 0;
    }

    /**
     * The record that this is for.
     * Note, implementations may make hard assumptions of the underlying class.
     *
     * @return Type of record, indicating the class of this record.
     */
    RecordType getType();

    /**
     * This method should format the string similarly to the following example.
     * <code>PlymouthRecord{flags=0, name='A record'}</code>
     *
     * @return The class and fields formatted for ease of reading.
     */
    String toString();
}
