package net.kjp12.plymouth.antixray.transformers;// Created 2021-03-07T02:30:01

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author KJP12
 * @since ${version}
 **/
class Transformers {
    static AbstractInsnNode walk(AbstractInsnNode from, int pop) {
        while (pop > 0) {
            pop -= stack(from = from.getPrevious());
        }
        return from;
    }

    static int stack(AbstractInsnNode node) {
        return switch (node.getOpcode()) {
            case NOP -> 0; // pop 0, push 0
            case ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH, LDC, ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> 1; // pop 0, push 1
            case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD -> -1; // pop 2, push 1
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> -1; // pop 1, push 0
            case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> -3; // pop 3, push 0
            case POP -> -1; // pop 1, push 0
            // case POP2 -> throw new Error("ub"); // pop 1 if long or double, pop 2 otherwise, push 0
            case DUP -> 1; // pop 1, push 2
            case DUP_X1 -> 1; // pop 2, push 3
            // case DUP_X2 -> throw new Error("ub"); // pop 2, push 3 if long or double, pop 3, push 4 otherwise
            // case DUP2 -> throw new Error("ub"); // pop 1, push 2 if long or double, pop 2, push 4 otherwise
            // case DUP2_X1 -> throw new Error("ub"); // pop 2, push 3 if long or double, pop 3, push 5 otherwise
            // case DUP2_X2 -> throw new Error("ub"); // See JVMS 6 # 6.5.dup2_x2
            case SWAP -> 0; // pop 2, push 2
            case IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM -> -1; // pop 2, push 1
            case INEG, LNEG, FNEG, DNEG -> 0; // pop 1, push 1
            case ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR -> -1; // pop 2, push 1
            case IINC, // pop 0, push 0
                    I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S -> 0; // pop 1, push 1
            case LCMP, FCMPL, FCMPG, DCMPL, DCMPG, // pop 2, push 1
                    IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> -1; // pop 1, push ?
            case RETURN -> 0; // pop 0, push ?
            case GETSTATIC -> 1; // pop 0, push 1
            case PUTSTATIC -> -1; // pop 1, push 0
            case GETFIELD -> 0; // pop 1, push 1
            case PUTFIELD -> -2; // pop 2, push 0
            case INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE -> -processInvokeVirtual((MethodInsnNode) node); // pop 1 + params, push r ?: 0
            case INVOKESTATIC -> -processInvokeStatic((MethodInsnNode) node);
            case NEW -> 1; // pop 0, push 1
            case NEWARRAY, ANEWARRAY, ARRAYLENGTH -> 0; // pop 1, push 1
            case ATHROW -> -1; // pop 1, push ?
            case CHECKCAST -> 0; // pop 1, push 1
            case MONITORENTER, MONITOREXIT -> -1; // pop 1, push 0
            case MULTIANEWARRAY -> -((MultiANewArrayInsnNode) node).dims + 1; // pop ?, push 1
            default -> throw new Error("ub");
        };
    }

    static int processInvokeVirtual(MethodInsnNode node) {
        var type = Type.getMethodType(node.desc);
        var args = type.getArgumentTypes().length;
        return type.getReturnType() == Type.VOID_TYPE ? args + 1 : args;
    }

    static int processInvokeStatic(MethodInsnNode node) {
        var type = Type.getMethodType(node.desc);
        var args = type.getArgumentTypes().length;
        return type.getReturnType() == Type.VOID_TYPE ? args : args - 1;
    }
}
