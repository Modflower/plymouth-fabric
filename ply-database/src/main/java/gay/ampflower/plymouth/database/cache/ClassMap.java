package gay.ampflower.plymouth.database.cache;

import org.objectweb.asm.Opcodes;

import java.sql.Timestamp;
import java.util.IdentityHashMap;

/**
 * @author Ampflower
 * @since ${version}
 **/
enum ClassMap {
    VOID(void.class, "setObject", "getObject", Opcodes.ALOAD, Opcodes.ASTORE, true, false),
    BOOLEAN(boolean.class, "setBoolean", "getBoolean", Opcodes.ILOAD, Opcodes.ISTORE),
    BYTE(byte.class, "setByte", "getByte", Opcodes.ILOAD, Opcodes.ISTORE),
    SHORT(short.class, "setShort", "getShort", Opcodes.ILOAD, Opcodes.ISTORE),
    INT(int.class, "setInt", "getInt", Opcodes.ILOAD, Opcodes.ISTORE),
    LONG(long.class, "setLong", "getLong", Opcodes.LLOAD, Opcodes.LSTORE),
    FLOAT(float.class, "setFloat", "getFloat", Opcodes.FLOAD, Opcodes.FSTORE),
    DOUBLE(double.class, "setDouble", "getDouble", Opcodes.DLOAD, Opcodes.DSTORE),
    STRING(String.class, "setString", "getString"),
    TIMESTAMP(Timestamp.class, "setTimestamp", "getTimestamp"),
    ;
    static final IdentityHashMap<Class<?>, ClassMap> intern = new IdentityHashMap<>();
    final Class<?> internal;
    final String setter, getter;
    final boolean passClass, tryBox;
    final int load, store;

    ClassMap(Class<?> internal, String setter, String getter, int load, int store, boolean passClass, boolean tryBox) {
        this.internal = internal;
        this.setter = setter;
        this.getter = getter;
        this.load = load;
        this.store = store;
        this.passClass = passClass;
        this.tryBox = tryBox;
    }

    ClassMap(Class<?> internal, String setter, String getter, int load, int store) {
        this(internal, setter, getter, load, store, false, false);
    }

    ClassMap(Class<?> internal, String setter, String getter) {
        this(internal, setter, getter, Opcodes.ALOAD, Opcodes.ASTORE, false, false);
    }

    static {
        for (var v : values()) {
            intern.put(v.internal, v);
        }
    }

    static ClassMap findMapper(Class<?> clazz) {
        return intern.getOrDefault(clazz, VOID);
    }
}
