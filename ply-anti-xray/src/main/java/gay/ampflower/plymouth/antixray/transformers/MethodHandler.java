package gay.ampflower.plymouth.antixray.transformers;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gay.ampflower.plymouth.antixray.transformers.Transformers.processIndy2;
import static gay.ampflower.plymouth.antixray.transformers.Transformers.processInvoke2;
import static org.objectweb.asm.Opcodes.*;

/**
 * Bytecode scour on a per-method basis.
 * <p>
 * The method is read in sequential order initially to build up a map of methods.
 * <p>
 * The scour first starts with reading the instructions, making consumption & fork nodes out of
 * any calls and instructions where applicable.
 * <p>
 * This makes assumptions about the JVM's inner functions, and assumes those to always be true
 * for static analysis of the bytecode.
 *
 * @author Ampflower
 * @since ${version}
 **/
class MethodHandler {
    private static final Type DYNCONST_TYPE = Type.getType(ConstantDynamic.class);
    private static final Type HANDLE_TYPE = Type.getType(Handle.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private final MethodNode method;
    private int si;
    private StackVar[] stack;
    private VarScope[] variables;

    public MethodHandler(MethodNode method) {
        this.method = method;
        this.stack = new StackVar[method.maxStack];
        this.variables = new VarScope[method.maxLocals];
    }

    void process() {
        for (var insn : method.instructions)
            switch (insn.getOpcode()) {
                case -1, NOP -> {
                }
                case ACONST_NULL -> push(new SingleStackVar(insn, Type.VOID_TYPE, null));
                case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> push(new SingleStackVar(insn, Type.INT_TYPE, insn.getOpcode() - ICONST_0));
                case LCONST_0, LCONST_1 -> push(new SingleStackVar(insn, Type.LONG_TYPE, insn.getOpcode() - LCONST_0));
                // Use JVM's consts for the following 5. Floating-point precision errors
                // should not be introduced here; these are constants, not runtime-computed values.
                case FCONST_0 -> push(new SingleStackVar(insn, Type.FLOAT_TYPE, 0F));
                case FCONST_1 -> push(new SingleStackVar(insn, Type.FLOAT_TYPE, 1F));
                case FCONST_2 -> push(new SingleStackVar(insn, Type.FLOAT_TYPE, 2F));
                case DCONST_0 -> push(new SingleStackVar(insn, Type.DOUBLE_TYPE, 0D));
                case DCONST_1 -> push(new SingleStackVar(insn, Type.DOUBLE_TYPE, 1D));
                case BIPUSH -> push(new SingleStackVar((IntInsnNode) insn, Type.BYTE_TYPE));
                case SIPUSH -> push(new SingleStackVar((IntInsnNode) insn, Type.SHORT_TYPE));
                case LDC -> push(new SingleStackVar((LdcInsnNode) insn));
                case ILOAD -> push(new SingleStackVar((VarInsnNode) insn, Type.INT_TYPE, variables));
                case LLOAD -> push(new SingleStackVar((VarInsnNode) insn, Type.LONG_TYPE, variables));
                case FLOAD -> push(new SingleStackVar((VarInsnNode) insn, Type.FLOAT_TYPE, variables));
                case DLOAD -> push(new SingleStackVar((VarInsnNode) insn, Type.DOUBLE_TYPE, variables));
                case ALOAD -> push(new SingleStackVar((VarInsnNode) insn, variables));
                case ISTORE -> ivar((VarInsnNode) insn, pop());
                case LSTORE -> lvar((VarInsnNode) insn, pop());
                case FSTORE -> fvar((VarInsnNode) insn, pop());
                case DSTORE -> dvar((VarInsnNode) insn, pop());
                case ASTORE -> avar((VarInsnNode) insn, pop());
                // TODO: Treat as special variable scope; changes in here *MAY* survive
                // case IALOAD,  LALOAD,  FALOAD,  DALOAD,  AALOAD,  BALOAD,  CALOAD,  SALOAD  ->;
                // case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE ->;
                case POP -> pop();
                case DUP -> dup1(insn);
                case DUP_X1 -> dupx1(insn);
                case DUP_X2 -> dupx2(insn);
                case DUP2 -> dup2(insn);
                case DUP2_X1 -> dup2x1(insn);
                case DUP2_X2 -> dup2x2(insn);
                case SWAP -> swap1();
                case INEG -> push(new CalculatedStackVar(insn, pop(1), Type.INT_TYPE));
                case LNEG -> push(new CalculatedStackVar(insn, pop(1), Type.LONG_TYPE));
                case FNEG -> push(new CalculatedStackVar(insn, pop(1), Type.FLOAT_TYPE));
                case DNEG -> push(new CalculatedStackVar(insn, pop(1), Type.DOUBLE_TYPE));
                case IINC -> { /* TODO: Mark as mutated */}
                case L2I, F2I, D2I -> cast((InsnNode) insn, Type.INT_TYPE);
                case I2L, F2L, D2L -> cast((InsnNode) insn, Type.LONG_TYPE);
                case I2F, L2F, D2F -> cast((InsnNode) insn, Type.FLOAT_TYPE);
                case I2D, L2D, F2D -> cast((InsnNode) insn, Type.DOUBLE_TYPE);
                case I2B -> cast((InsnNode) insn, Type.BYTE_TYPE);
                case I2C -> cast((InsnNode) insn, Type.CHAR_TYPE);
                case I2S -> cast((InsnNode) insn, Type.SHORT_TYPE);
                case IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> push(new CalculatedStackVar(insn, pop(2), Type.INT_TYPE));
                case LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> push(new CalculatedStackVar(insn, pop(2), Type.LONG_TYPE));
                case FADD, FSUB, FMUL, FDIV, FREM -> push(new CalculatedStackVar(insn, pop(2), Type.FLOAT_TYPE));
                case DADD, DSUB, DMUL, DDIV, DREM -> push(new CalculatedStackVar(insn, pop(2), Type.DOUBLE_TYPE));
                // TODO: Add type verification.
                case LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> push(new CalculatedStackVar(insn, pop(2), Type.INT_TYPE));
                case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> pop();
                case IFNULL, IFNONNULL -> pop();
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> pop(2);
                case IF_ACMPEQ, IF_ACMPNE -> pop(2);
                case GOTO -> {/* TODO: Sanity check & mark label as possible junction */}
                case JSR -> { /* Push address to stack & GOTO */ }
                case RET -> { /* Take address from variable & JMP */ }
                case TABLESWITCH -> pop();
                case LOOKUPSWITCH -> pop();
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> {
                    pop(); /* TODO: Break Scope */
                }
                case RETURN -> { /* TODO: Break Scope */ }
                case GETSTATIC -> push(new SingleStackVar(insn, Type.getType(((FieldInsnNode) insn).desc), null));
                // TODO: Mark as side-effect
                case PUTSTATIC -> pop();
                // TODO: Mark as side-effect
                case GETFIELD -> {
                }
                // TODO: Mark as side-effect
                case PUTFIELD -> pop(2);
                // TODO:
                case INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE, INVOKESTATIC -> processInvoke2((MethodInsnNode) insn); // pop s ?: 1 + params, push r ?: 0
                case INVOKEDYNAMIC -> processIndy2((InvokeDynamicInsnNode) insn);
                case NEW -> push(new SingleStackVar((TypeInsnNode) insn));
                // TODO: Convert to primitive
                // case NEWARRAY -> push(new SingleStackVar((VarInsnNode) insn, pop()));
                // case ANEWARRAY -> push(new SingleStackVar((TypeInsnNode) insn, pop()));
                // case MULTIANEWARRAY -> push(new SingleStackVar(, pop(((MultiANewArrayInsnNode) insn).dims)));
                // TODO: Verify if array.
                case ARRAYLENGTH -> push(new CalculatedStackVar(insn, pop(1), Type.INT_TYPE));
                case ATHROW -> { /* TODO: Break Scope */ }
                case CHECKCAST -> cast((TypeInsnNode) insn);
                case INSTANCEOF -> push(new CalculatedStackVar(insn, pop(1), Type.INT_TYPE));
                // TODO: Mark as side-effect
                case MONITORENTER -> pop();
                // TODO: Mark as side-effect
                case MONITOREXIT -> pop();
                default -> throw new Error("Undefined Behaviour: " + insn + ": " + insn.getOpcode());
            }
    }

    void push(StackVar var) {
        stack[si++] = var;
    }

    StackVar pop() {
        var top = stack[si];
        stack[si--] = null;
        return top;
    }

    StackVar[] pop(int amount) {
        var top = Arrays.copyOfRange(stack, si - amount, si);
        Arrays.fill(stack, si - amount, si, null);
        si -= amount;
        return top;
    }

    void swap1() {
        var var = stack[si];
        stack[si] = stack[si - 1];
        stack[si - 1] = var;
    }

    void dup1(AbstractInsnNode node) {
        var var = new DuplicatedStackVar(node, stack[si - 1]);
        stack[si++] = var;
    }

    void dupx1(AbstractInsnNode node) {
        var var = new DuplicatedStackVar(node, stack[si - 1]);
        stack[si + 1] = stack[si];
        stack[si] = stack[si - 1];
        stack[si - 1] = var;
        si++;
    }

    void dupx2(AbstractInsnNode node) {
        if (stack[si - 1].type().getSize() == 2) {
            // Virtually the same.
            dupx1(node);
        } else {
            var var = new DuplicatedStackVar(node, stack[si - 1]);
            System.arraycopy(stack, si - 2, stack, si - 1, 3);
            stack[si - 2] = var;
            si++;
        }
    }

    void dup2(AbstractInsnNode node) {
        if (stack[si - 1].type().getSize() == 2) {
            dup1(node);
        } else {
            var var1 = new DuplicatedStackVar(node, stack[si - 2]);
            var var2 = new DuplicatedStackVar(node, stack[si - 1]);
            stack[si++] = var1;
            stack[si++] = var2;
        }
    }

    void dup2x1(AbstractInsnNode node) {
        if (stack[si - 1].type().getSize() == 2) {
            dupx1(node);
        } else {
            var var1 = new DuplicatedStackVar(node, stack[si - 2]);
            var var2 = new DuplicatedStackVar(node, stack[si - 1]);
            System.arraycopy(stack, si - 3, stack, si - 1, 3);
            stack[si - 3] = var1;
            stack[si - 2] = var2;
            si += 2;
        }
    }

    void dup2x2(AbstractInsnNode node) {
        if (stack[si - 1].type().getSize() == 2) {
            if (stack[si - 2].type().getSize() == 2) {
                // form 4
                dupx1(node);
            } else {
                // form 2
                var var = new DuplicatedStackVar(node, stack[si - 1]);
                System.arraycopy(stack, si - 2, stack, si - 1, 3);
                stack[si - 2] = var;
                si++;
            }
        } else {
            if (stack[si - 3].type().getSize() == 2) {
                // form 3
                dup2x1(node);
            } else {
                // form 1
                var var1 = new DuplicatedStackVar(node, stack[si - 2]);
                var var2 = new DuplicatedStackVar(node, stack[si - 1]);
                System.arraycopy(stack, si - 4, stack, si - 2, 4);
                stack[si - 4] = var1;
                stack[si - 3] = var2;
                si += 2;
            }
        }
    }

    void cast(TypeInsnNode node) {
        int var = si - 1;
        stack[var] = new CastedStackVar(node, Type.getType(node.desc), stack[var]);
    }

    void cast(InsnNode node, Type type) {
        int var = si - 1;
        stack[var] = new CastedStackVar(node, type, stack[var]);
    }

    abstract class Scope {
        private final StackVar[] stack;
        private final VarScope[] locals;

        Scope(int stack, int locals) {
            this.stack = new StackVar[stack];
            this.locals = new VarScope[locals];
        }

        abstract StackVar stack(int i);

        abstract StackVar pop();

        abstract StackVar[] pop(int amount);
    }

    // class RootScope extends Scope {
    //     RootScope(int stack, int locals) {
    //         super(stack, locals);
    //     }
//
    // }

    interface StackVar {
        AbstractInsnNode insn();

        StackMut mut();

        Type type();
    }

    record SingleStackVar(AbstractInsnNode insn, Type type, Object raw) implements StackVar {
        SingleStackVar(VarInsnNode node, VarScope[] vars) {
            this(node, vars[node.var].type, vars[node.var]);
        }

        SingleStackVar(VarInsnNode node, Type type, VarScope[] vars) {
            this(node, type, vars[node.var]);
            if (!type.equals(((VarScope) raw).type)) {
                throw new AssertionError("Expected " + type + ", got " + raw);
            }
        }

        SingleStackVar(IntInsnNode node, Type type) {
            this(node, type, node.operand);
        }

        SingleStackVar(LdcInsnNode node) {
            this(node, determineLdc(node.cst), node.cst);
        }

        SingleStackVar(TypeInsnNode node) {
            this(node, Type.getType(node.desc), null);
        }

        public StackMut mut() {
            return StackMut.T0_1;
        }

        private static Type determineLdc(Object raw) {
            if (raw instanceof Integer) {
                return Type.INT_TYPE;
            } else if (raw instanceof Float) {
                return Type.FLOAT_TYPE;
            } else if (raw instanceof Long) {
                return Type.LONG_TYPE;
            } else if (raw instanceof Double) {
                return Type.DOUBLE_TYPE;
            } else if (raw instanceof String) {
                return STRING_TYPE;
            } else if (raw instanceof Type type) {
                if (type.getSort() == Type.METHOD) {
                    return type;
                }
                return CLASS_TYPE;
            } else if (raw instanceof Handle) {
                return HANDLE_TYPE;
            } else if (raw instanceof ConstantDynamic) {
                return DYNCONST_TYPE;
            }
            throw new IllegalArgumentException("Unknown LDC: " + raw);
        }
    }

    record CastedStackVar(AbstractInsnNode insn, Type type, StackVar extended) implements StackVar {
        public StackMut mut() {
            return StackMut.T0_0;
        }
    }

    record DuplicatedStackVar(AbstractInsnNode insn, StackVar var) implements StackVar {
        public StackMut mut() {
            return StackMut.T0_1;
        }

        public Type type() {
            return var.type();
        }
    }

    record CalculatedStackVar(AbstractInsnNode insn, StackVar[] vars, Type type) implements StackVar {
        @Override
        public StackMut mut() {
            return new StackMut(vars.length, 1);
        }
    }

    VarScope scope(VarInsnNode node, Type type) {
        var scope = variables[node.var];
        if (scope == null || !type.equals(scope.type)) {
            if (scope != null) scope.endOfScope = node.getPrevious();
            variables[node.var] = scope = new VarScope(type, node.var);
        }
        return scope;
    }

    void ivar(VarInsnNode node, StackVar var) {
        if (!Type.INT_TYPE.equals(var.type())) {
            throw new AssertionError("Excepted int, got " + var + " by " + node);
        }
        scope(node, Type.INT_TYPE).push(var);
    }

    void lvar(VarInsnNode node, StackVar var) {
        if (!Type.LONG_TYPE.equals(var.type())) {
            throw new AssertionError("Excepted long, got " + var + " by " + node);
        }
        scope(node, Type.LONG_TYPE).push(var);
    }

    void fvar(VarInsnNode node, StackVar var) {
        if (!Type.FLOAT_TYPE.equals(var.type())) {
            throw new AssertionError("Excepted float, got " + var + " by " + node);
        }
        scope(node, Type.FLOAT_TYPE).push(var);
    }

    void dvar(VarInsnNode node, StackVar var) {
        if (!Type.DOUBLE_TYPE.equals(var.type())) {
            throw new AssertionError("Excepted double, got " + var + " by " + node);
        }
        scope(node, Type.DOUBLE_TYPE).push(var);
    }

    void avar(VarInsnNode node, StackVar var) {
        scope(node, var.type()).push(var);
    }

    static class VarScope {
        final Type type;
        final int index;
        final List<StackVar> definitions;
        AbstractInsnNode endOfScope;

        VarScope(Type type, int index) {
            this.type = type;
            this.index = index;
            this.definitions = new ArrayList<>();
        }

        void push(StackVar var) {
            definitions.add(var);
        }
    }
}
