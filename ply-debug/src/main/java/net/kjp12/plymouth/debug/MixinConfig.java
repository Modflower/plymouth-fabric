package net.kjp12.plymouth.debug;// Created 2021-03-30T00:09:55

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Early riser, avoid calling net.minecraft and any mod classes that may cascade-load, <em>especially the various Plymouth modules</em>.
 *
 * @author KJP12
 * @since 0.0.0
 */
public class MixinConfig implements IMixinConfigPlugin {
    private static final Logger logger = LogManager.getLogger("Plymouth: Debug: Mixin");
    private int substr;

    @Override
    public void onLoad(String mixinPackage) {
        substr = mixinPackage.length() + 1;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var loader = FabricLoader.getInstance();
        var index = mixinClassName.indexOf('.', substr);
        String env, cur = mixinClassName.substring(substr, index);
        if (!Fusebox.isEnabled(cur)) {
            logger.info("Package `{}` disabled. If you want to enable this mixin, put `{1}=true` in pdb.fb.properties", cur);
            return false;
        }
        logger.info("Testing package `{}`...", cur);
        switch (cur) {
            case "anti_xray" -> {
                if (!loader.isModLoaded("plymouth-anti-xray")) return false;
                env = getEnvironment(mixinClassName, index);
            }
            case "database" -> {
                if (!loader.isModLoaded("plymouth-database")) return false;
                env = getEnvironment(mixinClassName, index);
            }
            case "tracker" -> {
                if (!loader.isModLoaded("plymouth-tracker")) return false;
                env = getEnvironment(mixinClassName, index);
            }
            default -> env = cur;
        }
        {
            var mixin = mixinClassName.substring(substr);
            if (!Fusebox.isEnabled(mixin)) {
                logger.info("Mixin `{}` disabled. If you want to enable this mixin, put `{:1}=true` in pdb.fb.properties", mixin);
                return false;
            }
            logger.info("Testing mixin `{}`...", mixin);
        }
        return checkEnvironment(loader, env);
    }

    private String getEnvironment(String in, int s) {
        int e = in.indexOf('.', ++s);
        return e == -1 ? "" : in.substring(s, e);
    }

    private boolean checkEnvironment(FabricLoader loader, String str) {
        return switch (str) {
            case "client" -> loader.getEnvironmentType() == EnvType.CLIENT;
            case "server" -> loader.getEnvironmentType() == EnvType.SERVER;
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        //noinspection SwitchStatementWithTooFewBranches - not fully implemented yet
        switch (mixinClassName.substring(substr)) {
            case "poison.MixinInventoryClear" -> {
                var insnList = new InsnList();
                for (MethodNode method : targetClass.methods) {
                    switch (method.name) {
                        case "size", "updateItems", "method_5439", "method_5438", "method_7384", "method_7391", "method_7380", "method_7381" -> {
                            continue;
                        }
                        default -> {
                            if (method.name.startsWith("get") || method.name.startsWith("is")) continue;
                        }
                    }
                    genInsnList(insnList, method.name + method.desc);
                    method.instructions.insert(insnList);
                }
            }
        }
    }

    private void genInsnList(InsnList list, String method) {
        list.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/kjp12/plymouth/debug/Debug", "logger", "Lorg/apache/logging/log4j/Logger;"));
        list.add(new LdcInsnNode("Called " + method));
        list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Throwable"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V", false));
        list.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "error", "(Ljava/lang/String;Ljava/lang/Throwable;)V"));
    }
}
