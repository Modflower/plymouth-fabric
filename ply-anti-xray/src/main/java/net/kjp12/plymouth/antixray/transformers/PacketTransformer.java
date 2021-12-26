package net.kjp12.plymouth.antixray.transformers;// Created 2021-11-12T16:30:30

import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Map;
import java.util.Set;

import static net.kjp12.plymouth.antixray.transformers.Transformers.logger;
import static net.kjp12.plymouth.antixray.transformers.Transformers.mkType;

/**
 * @author KJP12
 * @since ${version}
 **/
public class PacketTransformer implements Transformer {
    private static final Identifier NAME = new Identifier("plymouth-anti-xray", "packet-transformer");
    private final Map<MethodType, String> invokeVirtualMap;
    private final Set<String> classReferences;

    PacketTransformer(Set<String> classReferences, Map<MethodType, String> invokeVirtualMap) {
        this.classReferences = classReferences;
        this.invokeVirtualMap = invokeVirtualMap;
    }

    @Override
    public Identifier getName() {
        return NAME;
    }

    @Override
    public boolean handlesClass(String name, String transformedName) {
        return classReferences.remove(name);
    }

    @Override
    public boolean transform(ClassNode classNode, Flags flags) {
        boolean transformed = false;
        for (var method : classNode.methods) {
            for (var call : AsmUtils.findMatchingNodes(method, n -> n instanceof MethodInsnNode m &&
                    m.getOpcode() == Opcodes.INVOKEVIRTUAL && invokeVirtualMap.containsKey(mkType(m)))) {
                if (!(call instanceof MethodInsnNode m1)) {
                    new AssertionError("Unexpected node " + call).printStackTrace();
                    continue;
                }
                var old = m1.name;
                m1.name = invokeVirtualMap.get(mkType(m1));
                logger.info("Redirected {} in {} to {}", old, classNode.name, m1.name);
                transformed = true;
            }
        }
        return transformed;
    }
}
