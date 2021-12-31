package net.kjp12.plymouth.antixray.transformers;// Created 2021-02-07T03:26:47

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.kjp12.plymouth.antixray.transformers.Transformers.*;

/**
 * ASM Transformer for GudASM when available.
 *
 * @author KJP12
 * @since ${version}
 **/
public class GudAsmTransformer implements Transformer {
    private static final Identifier IDENTIFIER = new Identifier("plymouth-anti-xray", "action-result-transformer");
    // private MethodType constructor, world;
    private final Map<MethodType, String> redirectMap;
    private final Set<String> packetClasses, mutateClasses;

    GudAsmTransformer(Set<String> packetClasses, Set<String> mutateClasses, Map<MethodType, String> redirectMap) {
        this.packetClasses = packetClasses;
        this.mutateClasses = mutateClasses;
        this.redirectMap = redirectMap;
        logger.info("packetClasses = {}", packetClasses);
        logger.info("mutateClasses = {}", mutateClasses);
        logger.info("redirectMap = {}", redirectMap);
        // // "net.kjp12.plymouth.antixray.transformers.Stub
        // try (var stubIn = GudAsmTransformer.class.getResourceAsStream("Stub.class")) {
        //     var reader = new ClassReader(stubIn);
        //     var visitor = new ClassNode();
        //     reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        //     var methods = visitor.methods.stream().collect(Collectors.toMap(node -> node.name, node -> node));
        //     {
        //         var constructor = methods.get("actionResponse");
        //         var insn = constructor.instructions.getLast().getPrevious();
        //         if (insn.getOpcode() != Opcodes.INVOKESPECIAL)
        //             throw new AssertionError("Expected constructor invocation, got " + insn);
        //         this.constructor = mkType((MethodInsnNode) insn);
        //     }
        //     {
        //         var world = methods.get("world");
        //         var insn = world.instructions.getLast().getPrevious();
        //         if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL)
        //             throw new AssertionError("Excepted getBlockState, got " + insn);
        //         this.world = mkType((MethodInsnNode) insn);
        //     }
        // } catch (IOException ioe) {
        //     throw new Error("Failed to load Stub", ioe);
        // }
    }

    @Override
    public Identifier getName() {
        return IDENTIFIER;
    }

    @Override
    public boolean handlesClass(String name, String transformedName) {
        return true;
    }

    // TODO: Make this dup and jump aware
    @Override
    public boolean transform(ClassNode classNode, Flags flags) {
        boolean transformed = false;
        var loadMap = new Int2ObjectOpenHashMap<List<AbstractInsnNode>>();
        var storMap = new Int2ObjectOpenHashMap<List<AbstractInsnNode>>();
        var mutLoad = new Int2IntOpenHashMap();
        var pop = new int[1];
        // I'm going 'alright, find all instances of consuming classes', then 'find the variable of the specific classes
        // I want to change', then 'find all loads of the variable and make sure it's going to outputs I want to mutate
        // anyways, or is of a different type'
        for (var method : classNode.methods) {
            // logger.info("Poking at {}.{}", classNode.name, method.name);
            loadMap.clear();
            storMap.clear();
            mutLoad.clear();
            for (var load : AsmUtils.findMatchingNodes(method, node -> node instanceof VarInsnNode var && var.getOpcode() >>> 4 == 1)) {
                if (!(load instanceof VarInsnNode var)) {
                    new Throwable("Unexpected node " + load).printStackTrace();
                    continue;
                }
                if (var.var == 0) continue;
                loadMap.computeIfAbsent(var.var, i -> new ArrayList<>()).add(var);
            }
            for (var stor : AsmUtils.findMatchingNodes(method, node -> node instanceof VarInsnNode var && var.getOpcode() >>> 4 == 3)) {
                if (!(stor instanceof VarInsnNode var)) {
                    new Throwable("Unexpected node " + stor).printStackTrace();
                    continue;
                }
                if (var.var == 0) continue;
                storMap.computeIfAbsent(var.var, i -> new ArrayList<>()).add(var);
            }
            int unsafeBefore = Type.getArgumentTypes(method.desc).length;
            // int varAvailable = Math.max(loadMap.keySet().intStream().max().orElse(0), storMap.keySet().intStream().max().orElse(0)) + 1;
            // boolean unsafe = false;
            for (var $invoke : AsmUtils.findMatchingNodes(method, node -> node instanceof MethodInsnNode invoke && packetClasses.contains(invoke.owner))) {
                if (!($invoke instanceof MethodInsnNode invoke)) {
                    new Throwable("Unexpected node " + $invoke).printStackTrace();
                    continue;
                }
                // logger.info("Poking at {}.{}{} for {}.{}{}", classNode.name, method.name, method.desc, invoke.owner, invoke.name, invoke.desc);
                var args = Type.getArgumentTypes(invoke.desc);
                for (int i = 0, l = args.length; i < l; i++) {
                    if (mutateClasses.contains(args[i].getDescriptor())) {
                        var insn = walkBackwards($invoke, l - i);
                        if (insn instanceof VarInsnNode var) {
                            if (var.var == 0) {
                                logger.warn("This reference for {}.{}{} in {}.{}{}, ignoring", invoke.owner, invoke.name, invoke.desc, classNode.name, method.name, method.desc);
                                continue; // we cannot intercept on this.
                            }
                            mutLoad.put(var.var, 0);
                            // FIXME: Make this smarter, this may severely go wrong!
                            for (var load : loadMap.get(var.var)) {
                                pop[0] = 1;
                                var consumer = walkForward(load, pop);
                                if (consumer instanceof MethodInsnNode consumingMethod) {
                                    // TODO: Check if method is allowed to receive
                                    var consumerArgs = Type.getArgumentTypes(consumingMethod.desc);
                                    if (consumerArgs.length >= pop[0] && consumerArgs[consumerArgs.length - pop[0]].equals(args[i])) {
                                        logger.info("Redirecting {}.{}{} with var {}", consumingMethod.owner, consumingMethod.name, consumingMethod.desc, var.var);
                                    } else {
                                        logger.info("Unknown redirect {}.{}{} with var {}", consumingMethod.owner, consumingMethod.name, consumingMethod.desc, var.var);
                                    }
                                    // unsafe |= !consumerArgs[consumerArgs.length+pop[0]].equals(args[i]);
                                }
                            }
                            // if(unsafe) {
                            //     if(!mutLoad.containsKey(var.var)) {
                            //         mutLoad.put(var.var, varAvailable++);
                            //     }
                            // }
                        } else if (insn instanceof MethodInsnNode supplier) {
                            var redirect = redirectMap.get(mkType(supplier));
                            if (redirect != null) {
                                transformed = true;
                                var old = supplier.name;
                                supplier.name = redirect;
                                logger.info("Redirected {}.{}{} in {}.{}{} to {} for {}.{}{}", supplier.owner, old, supplier.desc, classNode.name, method.name, method.desc, supplier.name, invoke.owner, invoke.name, invoke.desc);
                            } else {
                                logger.info("Cannot redirect {}.{}{} in {}.{}{} for {}.{}{}", supplier.owner, supplier.name, supplier.desc, classNode.name, method.name, method.desc, invoke.owner, invoke.name, invoke.desc);
                            }
                        }
                    }
                }
            }
            // if(unsafe) {

            // } else {
            // for(var stor : storMap.get(var.var)) {
            //     var supplier = walkBackwards(stor, 1);
            //     if()
            // }
            // }
            var itr = mutLoad.keySet().intIterator();
            while (itr.hasNext()) {
                var var = itr.nextInt();
                if (!storMap.containsKey(var)) {
                    if (var <= unsafeBefore) {
                        logger.warn("Cannot redirect {} in {}.{}{} as it is purely a parameter.", var, classNode.name, method.name, method.desc);
                    } else {
                        logger.warn("Cannot redirect {} in {}.{}{} as it doesn't exist?! Did someone generate this class?", var, classNode.name, method.name, method.desc);
                    }
                } else for (var stor : storMap.get(var)) {
                    var $supplier = walkBackwards(stor, 1);
                    if ($supplier instanceof MethodInsnNode supplier) {
                        var redirect = redirectMap.get(mkType(supplier));
                        if (redirect != null) {
                            transformed = true;
                            var old = supplier.name;
                            supplier.name = redirect;
                            logger.info("Redirected {}.{}{} in {}.{}{} to {} for variable {}", supplier.owner, old, supplier.desc, classNode.name, method.name, method.desc, supplier.name, var);
                        } else {
                            logger.info("Cannot redirect {}.{}{} in {}.{}{} for variable {}", supplier.owner, supplier.name, supplier.desc, classNode.name, method.name, method.desc, var);
                        }
                    } else {
                        logger.info("Cannot redirect instruction {} for var {} in {}.{}{}", $supplier, var, classNode.name, method.name, method.desc);
                    }
                }
            }
        }
        return transformed;
    }
}
