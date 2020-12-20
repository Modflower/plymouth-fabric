package net.kjp12.helium;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * A note to maintainers: This file must <em>not</em> have any references to classes within Minecraft and other mods
 * that are not also an early riser, be it direct or indirect.
 * This class rises early via Mixin and can cause issues to mixins loaded in later.
 * <p>
 * Safe calls include java.*, sun.*, com.sun.*, {@link HeliumEarlyRiser}, {@link MixinConfig} and mods' early riser classes.
 * Any other call is unsafe and may cause failure conditions from classes loading without expected injections.
 *
 * @author KJP12
 * @since Dec. 19, 2020 @ 16:54
 **/
// TODO: More automated config system
public class MixinConfig implements IMixinConfigPlugin {
    // Quite crude of a config system, but should be sufficient.
    private boolean
            enableLocking = true,
            enableAntiXray = true,
            enableFallThroughPrevention = true;

    @Override
    public void onLoad(String mixinPackage) {
        // TODO: Some form of override API. Perhaps by the fabric.mod.json file if the loader rises early enough?
        try {
            Properties properties = new Properties();
            Path mixinConfig = HeliumEarlyRiser.config.resolve("helium.mixin.properties");
            if (Files.exists(mixinConfig)) {
                // Load in properties from disk if they exist. We'll dynamically set stuff as they come through.
                try (var stream = Files.newInputStream(mixinConfig)) {
                    properties.load(stream);
                }
                // We didn't crash, so, we should be good.
                if (properties.containsKey("locking"))
                    enableLocking = Boolean.parseBoolean(properties.getProperty("locking"));
                if (properties.containsKey("anti-xray"))
                    enableAntiXray = Boolean.parseBoolean(properties.getProperty("anti-xray"));
                if (properties.containsKey("prevent-fall-through"))
                    enableFallThroughPrevention = Boolean.parseBoolean(properties.getProperty("prevent-fall-through"));
            } else {
                Files.createDirectories(HeliumEarlyRiser.config);
                Files.createFile(mixinConfig);
                // The config doesn't exist. Default all.
                properties.put("locking", Boolean.toString(enableLocking));
                properties.put("anti-xray", Boolean.toString(enableAntiXray));
                properties.put("prevent-fall-through", Boolean.toString(enableFallThroughPrevention));
                try (var stream = Files.newOutputStream(mixinConfig)) {
                    properties.store(stream, "Helium Mixin Config");
                }
            }
        } catch (IOException ioe) {
            // TODO: More seamless handling. Tho, to be fair, if we get an IO exception, something's likely seriously wrong.
            throw new IOError(ioe);
        }
    }

    /**
     * no-op
     */
    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith("net.kjp12.helium.mixins.")) {
            HeliumEarlyRiser.LOGGER.warn("{} for {} is not owned by Helium. Defaulting to true.", mixinClassName, targetClassName);
            return true;
        }
        switch (mixinClassName.substring(24)) {
            // These are not covered by the config.
            case "AccessorBlockTag":
            case "MixinCommandManager":
                // This one must remain enabled to maintain NBT data.
            case "locking.MixinBlockEntity":
                return true;
            case "locking.MixinWorld":
            case "locking.entities.MixinEnderDragon":
            case "locking.entities.MixinEntity":
            case "locking.entities.MixinPlayerEntity":
            case "locking.entities.MixinTntMinecartEntity":
                return enableLocking;
            case "anti_xray.packets.s2c.MixinBlockUpdate":
            case "anti_xray.packets.s2c.MixinChunkData":
            case "anti_xray.packets.s2c.MixinChunkDeltaUpdateRecord":
            case "anti_xray.world.MixinWorldChunk":
                return enableAntiXray;
            case "misc.MixinPreventPlayerFallThrough":
                return enableFallThroughPrevention;
            default:
                HeliumEarlyRiser.LOGGER.warn("{} for {} was not caught by Helium's Mixin config. Defaulting to true.", mixinClassName, targetClassName);
                return true;
        }
    }

    /**
     * atm, no-op
     */
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    /**
     * no-op
     */
    @Override
    public List<String> getMixins() {
        return null;
    }

    /**
     * no-op
     */
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    /**
     * no-op
     */
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
