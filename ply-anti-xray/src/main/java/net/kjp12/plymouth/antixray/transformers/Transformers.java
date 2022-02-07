package net.kjp12.plymouth.antixray.transformers;// Created 2021-03-07T02:30:01

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.gudenau.minecraft.asm.api.v1.AsmInitializer;
import net.gudenau.minecraft.asm.api.v1.AsmRegistry;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import net.kjp12.hachimitsu.utilities.StreamStringSpliterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.jar.Manifest;

import static org.objectweb.asm.Opcodes.*;

/**
 * Utility & early riser transformer bootstrap for {@link GudAsmTransformer} and {@link PacketTransformer}.
 *
 * @author KJP12
 * @since ${version}
 **/
public class Transformers implements AsmInitializer {
    static final FabricLoader loader = FabricLoader.getInstance();
    static final ModContainer self = loader.getModContainer("plymouth-anti-xray").orElseThrow(AssertionError::new);
    static final Logger logger = LogManager.getLogger("Plymouth: Anti-Xray ASM Transformer");
    static final Type type = Type.getType("Lnet/kjp12/plymouth/antixray/transformers/Stub$MethodNameTo;");

    /**
     * Walks the stack until the ultimate consumer is found in accordance of the instruction's weight.
     *
     * @deprecated Use {@link #walkForward(AbstractInsnNode, int[])} as this implementation
     * does not account for total pop followed by push, leading to missed consumers.
     */
    @Deprecated(forRemoval = true)
    static AbstractInsnNode walkForward(AbstractInsnNode from, int pop) {
        while (pop > 0) {
            pop -= stack(from = from.getNext());
        }
        return from;
    }

    /**
     * Walks the stack until the consumer is found in accordance to the instruction's weight.
     *
     * @param from The instruction to start walking from.
     * @param pop  The amount to pop from stack. This will be set to the argument
     *             location on the consumer on return.
     * @return The instruction that consumed the entire stack.
     */
    static AbstractInsnNode walkForward(AbstractInsnNode from, int[] pop) {
        while (pop[0] > 0) {
            var mut = stack2(from = from.getNext());
            if (mut.pop(pop[0])) {
                return from;
            }
            pop[0] += mut.weight();
        }
        return from;
    }

    static AbstractInsnNode walkBackwards(AbstractInsnNode from, int pop) {
        while (pop > 0) {
            pop += stack(from = from.getPrevious());
        }
        return from;
    }

    /**
     * Returns the stack mutation of the instruction in terms of pop & push.
     */
    static StackMut stack2(AbstractInsnNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }

        return switch (node.getOpcode()) {
            case -1, NOP, IINC, RETURN, CHECKCAST /* CC: 1:1, virtual 0 */ -> StackMut.T0_0;
            case ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                    LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
                    BIPUSH, SIPUSH, LDC, ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, GETSTATIC,
                    NEW -> StackMut.T0_1;
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, PUTSTATIC,
                    POP, MONITORENTER, MONITOREXIT,
                    IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> StackMut.T1_0;
            case INEG, LNEG, FNEG, DNEG,
                    I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S,
                    GETFIELD, NEWARRAY, ANEWARRAY, ARRAYLENGTH -> StackMut.T1_1;
            case DUP /* Special casing rq. */ -> StackMut.T1_2;
            case PUTFIELD -> StackMut.T2_0;
            case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
                    IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB,
                    IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM,
                    ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
                    LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> StackMut.T2_1;
            case SWAP -> StackMut.T2_2;
            case DUP_X1 /* Special casing rq. */ -> StackMut.T2_3;
            case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> StackMut.T3_0;
            case MULTIANEWARRAY -> new StackMut(((MultiANewArrayInsnNode) node).dims, 1);
            case INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE, INVOKESTATIC -> processInvoke2((MethodInsnNode) node); // pop s ?: 1 + params, push r ?: 0
            case INVOKEDYNAMIC -> processIndy2((InvokeDynamicInsnNode) node);
            default -> throw new Error("ub");
        };
    }

    /**
     * Returns the total weight of the instruction in terms of pop & push.
     *
     * @deprecated Use {@link #stack2(AbstractInsnNode)} as this method doesn't allow finding the consumer.
     */
    @Deprecated(forRemoval = true)
    static int stack(AbstractInsnNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        // if(node instanceof LineNumberNode || node instanceof FrameNode) return 0; // op: -1, not required for operation.
        return switch (node.getOpcode()) {
            case NOP, -1, // pop 0, push 0
                    SWAP, // pop 2, push 2
                    INEG, LNEG, FNEG, DNEG, // pop 1, push 1
                    IINC, // pop 0, push 0
                    I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S, // pop 1, push 1
                    RETURN, // pop 0, push ?
                    GETFIELD, // pop 1, push 1
                    NEWARRAY, ANEWARRAY, ARRAYLENGTH, CHECKCAST -> 0; // pop 1, push 1
            case ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                    LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
                    BIPUSH, SIPUSH, LDC, ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, GETSTATIC,
                    NEW, // pop 0, push 1
                    DUP, // pop 1, push 2
                    DUP_X1 -> -1; // pop 2, push 3
            case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, // pop 2, push 1
                    ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, PUTSTATIC, // pop 1, push 0
                    POP, // pop 1, push 0
                    MONITORENTER, MONITOREXIT, // pop 1, push 0
                    IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB,
                    IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM,
                    ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
                    LCMP, FCMPL, FCMPG, DCMPL, DCMPG, // pop 2, push 1
                    IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> 1; // pop 1, push ?
            case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> 3; // pop 3, push 0
            case PUTFIELD -> 2; // pop 2, push 0
            // case POP2 -> throw new Error("ub"); // pop 1 if long or double, pop 2 otherwise, push 0
            // case DUP_X2 -> throw new Error("ub"); // pop 2, push 3 if long or double, pop 3, push 4 otherwise
            // case DUP2 -> throw new Error("ub"); // pop 1, push 2 if long or double, pop 2, push 4 otherwise
            // case DUP2_X1 -> throw new Error("ub"); // pop 2, push 3 if long or double, pop 3, push 5 otherwise
            // case DUP2_X2 -> throw new Error("ub"); // See JVMS 6 # 6.5.dup2_x2
            case MULTIANEWARRAY -> ((MultiANewArrayInsnNode) node).dims - 1; // pop ?, push 1
            case INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE, INVOKESTATIC -> processInvoke((MethodInsnNode) node); // pop s ?: 1 + params, push r ?: 0
            case INVOKEDYNAMIC -> processIndy((InvokeDynamicInsnNode) node);
            default -> throw new Error("ub");
        };
    }

    /**
     * Returns a stack weight dependent on if the method's instanced, and the
     * method's descriptor.
     *
     * @deprecated Use {@link #processInvoke2(MethodInsnNode)} as this method doesn't allow finding the consumer.
     */
    @Deprecated(forRemoval = true)
    static int processInvoke(MethodInsnNode node) {
        return (Type.getReturnType(node.desc) != Type.VOID_TYPE ? -1 : 0) -
                Type.getArgumentTypes(node.desc).length -
                (node.getOpcode() != Opcodes.INVOKESTATIC ? 1 : 0); // pop 1 + args, push v?0:1
    }

    /**
     * Returns a stack weight dependent on the method's descriptor.
     *
     * @deprecated Use {@link #processIndy2(InvokeDynamicInsnNode)} as this method doesn't allow findind the consumer.
     */
    @Deprecated(forRemoval = true)
    static int processIndy(InvokeDynamicInsnNode node) {
        return (Type.getReturnType(node.desc) != Type.VOID_TYPE ? -1 : 0) -
                Type.getArgumentTypes(node.desc).length; // pop args, push v?0:1
    }

    /**
     * Returns a stack push & pop dependent on if the method is instanced and the
     * method's descriptor.
     * <p>
     * Pop is calculated by the descriptor length, adding 1 if instanced.
     * <p>
     * Push is 0 if the return is void, 1 otherwise.
     *
     * @param node The method node to convert to StackMut
     * @return The StackMut of the given method node.
     */
    static StackMut processInvoke2(MethodInsnNode node) {
        return new StackMut(
                (node.getOpcode() != Opcodes.INVOKESTATIC ? 1 : 0) + Type.getArgumentTypes(node.desc).length,
                Type.getReturnType(node.desc) != Type.VOID_TYPE ? 1 : 0);
    }

    /**
     * Returns pop of descriptor length, push of 0 if void, 1 otherwise.
     */
    static StackMut processIndy2(InvokeDynamicInsnNode node) {
        return new StackMut(
                Type.getArgumentTypes(node.desc).length,
                Type.getReturnType(node.desc) != Type.VOID_TYPE ? 1 : 0
        );
    }

    /**
     * Returns true if the method node is equivalent to the given method type.
     */
    static boolean matches(MethodInsnNode node, MethodType methodType) {
        if (node == null || methodType == null || !node.name.equals(methodType.getName()))
            return false;
        return Type.getMethodType(node.desc).equals(methodType.getDescriptor());
    }

    /**
     * Converts a given method node to a method type.
     */
    static MethodType mkType(MethodInsnNode methodInsn) {
        return new MethodType(Type.getType('L' + methodInsn.owner + ';'), methodInsn.name, Type.getMethodType(methodInsn.desc));
    }

    @Override
    public void onInitializeAsm() {
        UnaryOperator<String> preprocessor = null;
        // This ideally only needs to happen under the development environment.
        if (loader.isDevelopmentEnvironment()) {
            try (var rawManifest = Files.newInputStream(self.getPath("/META-INF/MANIFEST.MF"))) {
                var self = new Manifest(rawManifest);
                // Fabric-Mapping-Namespace: intermediary
                var compiledNamespace = self.getMainAttributes().getValue("Fabric-Mapping-Namespace");

                // If it is null, we are running in the native development environment, or as the dev jar.
                // If it is not null, but the manifest namespace does *not* match the runtime namespace,
                // we will have to remap from the manifest namespace to runtime namespace.
                if (compiledNamespace != null) {
                    var mappingResolver = loader.getMappingResolver();
                    var runtimeNamespace = mappingResolver.getCurrentRuntimeNamespace();
                    if (!compiledNamespace.equals(runtimeNamespace)) {
                        preprocessor = s -> mappingResolver.unmapClassName(compiledNamespace,
                                s.replace('/', '.'));
                    }
                }
            } catch (IOException ioe) {
                logger.warn("Unable to determine environment. Assuming same environment.", ioe);
            }
        }

        var asmString = preprocessor != null ? preprocessor.andThen(Transformers::binToAsm) : null;
        var binString = Objects.requireNonNullElse(preprocessor, Transformers::asmToBin);

        var stubMap = mkMap("Stub.class");

        var gud = AsmRegistry.getInstance();
        gud.registerTransformer(new GudAsmTransformer(
                getTransformerClassSet("asm/PacketTransformer.sys", asmString),
                getTransformerClassSet("asm/PacketTargets.sys", asmString),
                stubMap));
        gud.registerTransformer(new PacketTransformer(getTransformerClassSet("asm/PacketTransformer.sys", binString), stubMap));
    }

    private static String asmToBin(String path) {
        return path.replace('/', '.');
    }

    private static String binToAsm(String path) {
        return path.replace('.', '/');
    }

    /**
     * Creates a transformer class set based off the given assembly name file.
     * The output will be either internal names or descriptors, using the mutation
     * described by the preprocessor if one is present.
     * <p>
     * Trivia: The reason {@code ;} was picked is that it's the only universally reserved character
     * in the JVM that literally means end of string. {@code NUL} is technically valid.
     *
     * @param path         The path of the internal name or descriptor list.
     * @param preprocessor The string preprocessor required for the set output.
     * @return The internal name or descriptor list as found in the file, or transformed by preprocessor.
     * @implSpec The list found within the file is separated by {@code ;}. If the list starts with {@code ;},
     * the entire list is treated as a list of descriptors. Else, it is treated as a list of internal names.
     */
    private static Set<String> getTransformerClassSet(String path, Function<String, String> preprocessor) {
        var set = new HashSet<String>();
        try (var classSet = Files.newInputStream(self.getPath(path))) {
            // 1024 should be *plenty*. If anyone exceeds this, they really must like long paths, or stories.
            // The maximum length by both ZIP and the JVM is 65535, with a real of 65529 as the `.class`
            // extension is required. However, Windows enforces about 260, which means the real count for classes is 254.
            // Due to Windows' limit, for one to even reach 1024, you'd have to be ignoring unzipping support.
            var spliterator = new StreamStringSpliterator(classSet, new byte[]{';'}, false); //, 1024);
            // next required for seeking
            spliterator.next();
            spliterator.backtrack();
            boolean isDescriptor = spliterator.currentIndex() > 0;
            while (spliterator.hasNext()) {
                // spliterator.next();
                // spliterator.replace((byte)'/', (byte)'.');
                var out = spliterator.nextString();
                if (isDescriptor) {
                    if (preprocessor != null) {
                        var l = out.indexOf('L') + 1;
                        out = out.substring(0, l) + preprocessor.apply(out.substring(l)) + ';';
                    } else {
                        out += ';';
                    }
                } else {
                    if (preprocessor != null) {
                        out = preprocessor.apply(out);
                    }
                }
                set.add(out);
                // set.add(preprocessor.apply(spliterator.nextString()));
            }
        } catch (IOException ioe) {
            logger.warn("Failed to load transformer {}", path, ioe);
            throw new ExceptionInInitializerError(ioe);
        }
        return set;
    }

    /**
     * Creates a map of entire method to replacement name. Expects the file to be
     * in the similar manner of {@link Stub}, which is the following:
     *
     * <pre>
     * &#64;MethodNameTo("plymouth$getShadowBlock")
     * BlockState world(BlockView world, BlockPos pos) {
     *   return world.getBlockState(pos);
     * }
     * </pre>
     *
     * @param resource The stub class to read.
     * @return The map of method call to replacement name as annotated.
     */
    private static Map<MethodType, String> mkMap(String resource) {
        Map<MethodType, String> map = new HashMap<>();
        try (var stubIn = Transformers.class.getResourceAsStream(resource)) {
            if (stubIn == null) throw new IOException("Resource not found: " + resource);
            var reader = new ClassReader(stubIn);
            var visitor = new ClassNode();
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (var method : visitor.methods) {
                var redirectName = AsmUtils.getAnnotation(method, type);
                if (redirectName.isEmpty()) continue;
                var anno = redirectName.get();
                var annoValue = anno.values.indexOf("value");
                if (annoValue == -1) continue;
                var insn = method.instructions.getLast().getPrevious();
                if (!(insn instanceof MethodInsnNode invoke)) continue;
                map.put(mkType(invoke), (String) anno.values.get(annoValue + 1));
            }
        } catch (IOException ioe) {
            throw new Error("Failed to load " + resource, ioe);
        }
        return map;
    }
}
