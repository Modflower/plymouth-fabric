package net.kjp12.plymouth.database.cache;// Created 2021-22-06T05:23:53

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kjp12.hachimitsu.utilities.StringUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

// < -> CTX L*.0 -> 1, *LOAD  \d + 4 ; +4 offset for required variables, the selected opcode is dependent on the variable at call time.
// > -> CTX L*.0 -> 2, *STORE \d + 4 ; ^ See < - *LOAD doc.
// ? -> CTX L*.1 -> 0, IFNUL         ; C0 intrinsic candidate
// ^ -> CTX L*.0 -> 0, ALOAD  3
// ( -> CTX L*.0 -> 0, PUSH   L2
// ) -> CTX L2.0 -> 0, POP    L2     ; Implicit break on L1
// , -> CTX L1.0 -> 0, POP    L1
// . -> CTX L*.* -> 0, DREF   CTX
//\0 -> CTX L*.* ->  , POP    *      ; EOF, must pop or crash

// Allowed off of L*.0 = "<>^.(\0"
// Allowed off of L*.1 = "?.\0"
// Allowed off of L*.2 = ".\0"

// Allowed off of L1.0 = "<>^.,()\0"

// <0?^.worldIndex(causeWorld)>0

// Given above, the first encounter of both 0 and carrot will compile to the following ASM

// ;..., var4 = (var3 = (PlymouthPostgres) this.provider).worldIndex(var1.causeWorld), ...
// ALOAD            0
// GETFIELD         handlers/StatementHandler   provider    Lhandlers/SqlConnectionProvider;
// CHECKCAST        handlers/PlymouthPostgres
// DUP
// ASTORE           3
// ALOAD            1
// GETFIELD         records/LookupRecord        causeWorld  Lnet/minecraft/world/World;
// INVOKEVIRTUAL    handlers/PlymouthPostgres   worldIndex  (Lnet/minecraft/world/World;)I
// DUP
// ISTORE           4

// If carrot was already initialised, the ASM may automatically simply down.

// ;..., var4 = var3.worldIndex(var1.causeWorld), ...
// ALOAD            3
// ALOAD            1
// GETFIELD         records/LookupRecord        causeWorld  Lnet/minecraft/world/World;
// INVOKEVIRTUAL    handlers/PlymouthPostgres   worldIndex  (Lnet/minecraft/world/World;)I
// DUP
// ISTORE           4

// If variable 4 was already initialised, the ASM will be massively simplified.

// ;..., var4, ...
// ILOAD            4

/**
 * SQL Submitter Compiler. Expects a certain structure, do not attempt to use.
 *
 * @author KJP12
 * @since ${version}
 **/
class C0 implements Opcodes {
    private static final char[]
            T_NONE = StringUtils.createCharHashArray("<>^.,()\0"),
            T_LOAD = StringUtils.createCharHashArray("?.,)\0"),
            T_STORE = StringUtils.createCharHashArray(".,)\0");

    private static final String
            statementHandler = Type.getInternalName(StatementHandler.class),
            providerName = Type.getInternalName(SqlConnectionProvider.class);

    private static final int
            C_NONE = 0,
            C_LOAD = 1,
            C_STORE = 2;

    private int ia, ib, is;
    private String value;

    int index = 0;
    boolean carrot = false;
    Int2ObjectOpenHashMap<Class<?>> locals = new Int2ObjectOpenHashMap<>();
    Class<?> fallback;
    Class<? extends SqlConnectionProvider> sqlImpl;

    C0(Class<?> fallback, Class<? extends SqlConnectionProvider> sqlImpl) {
        this.fallback = fallback;
        this.sqlImpl = sqlImpl;
    }

    void compile(MethodVisitor submit, String value) throws NoSuchFieldException, NoSuchMethodException {
        // Load statement into stack.
        submit.visitVarInsn(ALOAD, 2);
        // Load the index onto the stack.
        if (++index <= 5) {
            submit.visitInsn(ICONST_0 + index);
        } else {
            submit.visitIntInsn(BIPUSH, index);
        }

        this.value = value;
        ia = ib = -1;
        var mapper = ClassMap.findMapper(l1(submit, false));
        submit.visitMethodInsn(INVOKEINTERFACE, "java/sql/PreparedStatement", mapper.setter, "(I" + (mapper == ClassMap.VOID ? "Ljava/lang/Object;" : mapper.internal.descriptorString()) + ")V", true);
    }

    /**
     * Linear single-return compiler
     *
     * @param submit The visitor
     * @param nested If this is nested within a method call.
     * @return The last class context. May be {@link Void#TYPE void.class}.
     */
    private Class<?> l1(MethodVisitor submit, boolean nested) throws NoSuchMethodException, NoSuchFieldException {
        Class<?> context = void.class;
        while (step()) {
            char e = ib >= value.length() ? '\0' : value.charAt(ib);
            boolean dropL1 = e == ')' || e == ',';
            if (!nested && dropL1)
                throw new IllegalArgumentException(value + " @ " + ib + " `" + e + "` not valid for unnested. " + this);
            switch (is) {
                case C_NONE -> {
                    switch (e) {
                        case '<' -> is = C_LOAD;
                        case '>' -> is = C_STORE;
                        case '^' -> {
                            lc(submit);
                            context = sqlImpl;
                        }
                        case '.', ',', ')', '\0' -> {
                            if (ib - ia > 1) {
                                if (context == void.class) {
                                    submit.visitVarInsn(ALOAD, 1);
                                    context = fallback;
                                }
                                context = vf(submit, context.getField(value.substring(ia, ib)));
                            }
                        }
                        case '(' -> {
                            if (ib - ia <= 1)
                                throw new IllegalStateException(this.toString());
                            if (context == void.class) {
                                submit.visitVarInsn(ALOAD, 1);
                                context = fallback;
                            }
                            var name = value.substring(ia, ib);
                            var params = l2(submit);
                            var m = Arrays.stream(context.getMethods())
                                    .filter(method -> method.getName().equals(name) && method.getParameterCount() == params.length)
                                    .filter(method -> {
                                        var other = method.getParameterTypes();
                                        for (int i = 0; i < other.length; i++) {
                                            if (!other[i].isAssignableFrom(params[i])) return false;
                                        }
                                        return true;
                                    }).findFirst().get();
                            context = vm(submit, m);
                        }
                    }
                }
                case C_LOAD -> {
                    int v = Integer.parseInt(value.substring(ia, ib));
                    switch (e) {
                        case '?' -> {
                            var clazz = locals.get(v);
                            if (clazz != null) {
                                submit.visitVarInsn(ClassMap.findMapper(clazz).load, v + 4);
                                // TODO: does not support method calls
                                var ne = value.indexOf(';');
                                ib = ne == -1 ? value.length() : ne;
                                return clazz;
                            }
                        }
                        case '.', ',', ')', '\0' -> {
                            context = locals.get(v);
                            if (context == null) throw new IllegalStateException("local " + v + " not stored " + this);
                            submit.visitVarInsn(ClassMap.findMapper(context).load, v + 4);
                        }
                    }
                    is = C_NONE;
                }
                case C_STORE -> {
                    if (context == void.class) throw new IllegalStateException("attempted store on void " + this);
                    int v = Integer.parseInt(value.substring(ia, ib));
                    // Originally matches `.`, `,`, `)`
                    locals.put(v, context);
                    submit.visitInsn(DUP);
                    submit.visitVarInsn(ClassMap.findMapper(context).store, v + 4);
                    is = C_NONE;
                }
            }
            if (dropL1) {
                is = C_NONE;
                return context;
            }
        }
        return context;
    }

    /**
     * Multi-return Parameter compiler.
     *
     * @param submit The visitor
     * @return Array of classes for parameters.
     */
    private Class<?>[] l2(MethodVisitor submit) throws NoSuchFieldException, NoSuchMethodException {
        var list = new ArrayList<Class<?>>();
        while (ib < value.length() && value.charAt(ib) != ')') {
            var clazz = l1(submit, true);
            if (clazz != void.class) {
                list.add(clazz);
            }
        }
        return list.toArray(new Class<?>[0]);
    }

    // private int p0(String value, int ia, int il) {
    //     int s = 1;
    //     while(s > 0) {
    //         ia = StringUtils.seekToDelimiter(value, method, il, ia);
    //         s += (value.charAt(ia) == '(' ? 1 : -1);
    //         if(++ia >= il) break;
    //     }
    //     if(s != 0) throw new IllegalArgumentException("Cannot parse " + value + ": " + ia + " -> " + il + ": uncapped parameters; current stack = " + s);
    //     return ia - 1;
    // }

    /**
     * Load carrot into the visitor.
     * <p>
     * If this is the first carrot visit, bytecode will be emitted to get the provider, cast it and store it at variable 3.
     *
     * @param submit The visitor.
     */
    private void lc(MethodVisitor submit) {
        if (carrot) {
            submit.visitVarInsn(ALOAD, 3);
        } else {
            carrot = true;
            submit.visitVarInsn(ALOAD, 0);
            submit.visitFieldInsn(GETFIELD, statementHandler, "provider", 'L' + providerName + ';');
            submit.visitTypeInsn(CHECKCAST, Type.getInternalName(sqlImpl));
            submit.visitInsn(DUP);
            submit.visitVarInsn(ASTORE, 3);
        }
    }

    /**
     * Visits method using parameters obtained from reflective access.
     *
     * @param submit The visitor
     * @param method The method to convert to a raw ASM call.
     * @return The return type of the method. May be {@link Void#TYPE void.class}.
     */
    private Class<?> vm(MethodVisitor submit, Method method) {
        var mods = method.getModifiers();
        var dec = method.getDeclaringClass();
        submit.visitMethodInsn((mods & ACC_STATIC) != 0 ? INVOKESTATIC : (mods & ACC_ABSTRACT) != 0 ? INVOKEINTERFACE : INVOKEVIRTUAL,
                Type.getInternalName(dec), method.getName(), Type.getMethodDescriptor(method), (dec.getModifiers() & ACC_INTERFACE) != 0);
        return method.getReturnType();
    }

    /**
     * Visit field using parameters obtained from reflective access.
     *
     * @param submit The visitor
     * @param field  The field to convert to a raw ASM call.
     * @return The return type of the field. Cannot ever be null or void.
     */
    private Class<?> vf(MethodVisitor submit, Field field) {
        var ret = field.getType();
        submit.visitFieldInsn((field.getModifiers() & ACC_STATIC) != 0 ? GETSTATIC : GETFIELD, Type.getInternalName(field.getDeclaringClass()), field.getName(), ret.descriptorString());
        return ret;
    }

    private boolean step() {
        if (ib >= value.length()) return false;
        ib = StringUtils.seekToDelimiter(value, switch (is) {
            case C_NONE -> T_NONE;
            case C_LOAD -> T_LOAD;
            case C_STORE -> T_STORE;
            default -> throw new IllegalStateException(is + " is an invalid state; expected 0 - 3");
        }, value.length(), ia = ib + 1);
        return true;
    }

    @Override
    public String toString() {
        return "C0{" +
                "ia=" + ia +
                ", ib=" + ib +
                ", is=" + is +
                ", value='" + value + '\'' +
                ", index=" + index +
                ", carrot=" + carrot +
                ", locals=" + locals +
                ", fallback=" + fallback +
                ", sqlImpl=" + sqlImpl +
                '}';
    }
}
