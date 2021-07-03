package net.kjp12.plymouth.antixray.transformers;// Created 2021-02-07T03:26:47

import net.gudenau.minecraft.asm.api.v1.*;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.util.stream.Collectors;

import static net.kjp12.plymouth.antixray.transformers.Transformers.walk;

/**
 * ASM Transformer for GudASM when available.
 *
 * @author KJP12
 * @since ${version}
 **/
public class GudAsmTransformer implements Transformer, AsmInitializer {
    private static final Identifier IDENTIFIER = new Identifier("plymouth-anti-xray", "action-result-transformer");
    private MethodType constructor, world;

    @Override
    public void onInitializeAsm() {
        // "net.kjp12.plymouth.antixray.transformers.Stub"
        try (var stubIn = GudAsmTransformer.class.getResourceAsStream("Stub.class")) {
            var reader = new ClassReader(stubIn);
            var visitor = new ClassNode();
            reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            var methods = visitor.methods.stream().collect(Collectors.toMap(node -> node.name, node -> node));
            {
                var constructor = methods.get("actionResponse");
                var insn = constructor.instructions.getLast().getPrevious();
                if (insn.getOpcode() != Opcodes.INVOKESPECIAL)
                    throw new AssertionError("Expected constructor invocation, got " + insn);
                var methodInsn = (MethodInsnNode) insn;
                this.constructor = new MethodType(Type.getType('L' + methodInsn.owner + ';'), methodInsn.name, Type.getMethodType(methodInsn.desc));
            }
            {
                var world = methods.get("world");
                var insn = world.instructions.getLast().getPrevious();
                if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL)
                    throw new AssertionError("Excepted getBlockState, got " + insn);
                var methodInsn = (MethodInsnNode) insn;
                this.world = new MethodType(Type.getType('L' + methodInsn.owner + ';'), methodInsn.name, Type.getMethodType(methodInsn.desc));
            }
        } catch (IOException ioe) {
            throw new Error("Failed to load Stub", ioe);
        }
        AsmRegistry.getInstance().registerTransformer(this);
    }

    @Override
    public Identifier getName() {
        return IDENTIFIER;
    }

    @Override
    public boolean handlesClass(String name, String transformedName) {
        return true;
    }

    @Override
    public boolean transform(ClassNode classNode, Flags flags) {
        boolean transformed = false;
        for (var method : classNode.methods) {
            for (var call : AsmUtils.findMethodCalls(method, 0, Opcodes.INVOKESPECIAL, constructor)) {
                var node = walk(call, 3).getPrevious(); // up 3 arguments then up 1 to invoke
                if (!(node instanceof MethodInsnNode m1)) {
                    new Throwable("Unexpected node " + node).printStackTrace();
                    break;
                }
                if (!matches(m1, world)) {
                    new Throwable("Unexpected call " + m1.owner + '.' + m1.name + m1.desc).printStackTrace();
                    break;
                }
                m1.name = "plymouth$getShadowBlock";
                transformed = true;
            }
        }
        return transformed;
    }

    private static boolean matches(MethodInsnNode node, MethodType methodType) {
        // DUCK time
        // if(!node.owner.equals(methodType.getOwner().getInternalName()))
        //     return false;
        if (!node.name.equals(methodType.getName()))
            return false;
        return Type.getMethodType(node.desc).equals(methodType.getDescriptor());
    }
}
