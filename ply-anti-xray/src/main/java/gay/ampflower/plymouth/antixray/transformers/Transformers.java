package gay.ampflower.plymouth.antixray.transformers;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.gudenau.minecraft.asm.api.v1.AsmInitializer;
import net.gudenau.minecraft.asm.api.v1.AsmRegistry;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import gay.ampflower.hachimitsu.utilities.StreamStringSpliterator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Ampflower
 * @since ${version}
 **/
public class Transformers implements AsmInitializer {
    static final FabricLoader loader = FabricLoader.getInstance();
    static final ModContainer self = loader.getModContainer("plymouth-anti-xray").orElseThrow(AssertionError::new);
    static final Logger logger = LoggerFactory.getLogger("Plymouth: Anti-Xray ASM Transformer");
    static final Type type = Type.getType("Lgay/ampflower/plymouth/antixray/transformers/Stub$MethodNameTo;");

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

    /**
     * Walks the stack until the producer is found in accordance to the instruction's weight.
     *
     * @param from The instruction to start walking from.
     * @param pop  The amount that has been popped from stack.
     * @return The instruction that produced the stack variable.
     */
    static AbstractInsnNode walkBackwards(AbstractInsnNode from, int pop) {
        while (pop > 0) {
            var mut = stack2(from = from.getPrevious());
            if (mut.push(pop)) {
                return from;
            }
            pop -= mut.weight();
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
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
                    IF_ACMPEQ, IF_ACMPNE -> StackMut.Ti_0;
            default -> throw new Error("Undefined Behaviour: " + node + ": " + node.getOpcode());
        };
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
            var manifest = self.findPath("/META-INF/MANIFEST.MF");
            if (manifest.isPresent())
                try (var rawManifest = Files.newInputStream(manifest.get())) {
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
        try (var classSet = Files.newInputStream(self.findPath(path).orElseThrow())) {
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
