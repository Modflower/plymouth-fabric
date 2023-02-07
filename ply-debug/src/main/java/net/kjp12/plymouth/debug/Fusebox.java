package net.kjp12.plymouth.debug;// Created 2021-14-07T22:07:03

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/**
 * @author KJP12
 * @since ${version}
 **/
public class Fusebox {
    private static final Properties properties = new Properties();
    private static boolean defaultValue;

    static {
        // Actually initialises the values.
        reinit();
    }

    public static boolean isEnabled(String option) {
        return getBoolean(option, defaultValue);
    }

    public static boolean getBoolean(String option, boolean def) {
        String str = properties.getProperty(option);
        if (str != null && !str.isBlank()) {
            if ("true".equalsIgnoreCase(str)) {
                return true;
            }
            if ("false".equalsIgnoreCase(str)) {
                return false;
            }
        }
        return def;
    }

    public static int getInteger(String option, int def) {
        try {
            String str = properties.getProperty(option);
            if (str != null && !str.isBlank()) {
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException ignored) {
        }
        return def;
    }

    // Anti-Xray
    public static boolean viewAntiXraySet, viewAntiXrayUpdate, viewAntiXrayTest, viewBlockDelta, viewBlockEvent,
            viewBlockEntityUpdate, viewChunkLoad, viewChunkBlockEntity;
    public static int viewAntiXraySetLimit, viewAntiXrayUpdateLimit, viewAntiXrayTestLimit, viewBlockDeltaLimit,
            viewBlockEventLimit, viewBlockEntityUpdateLimit, viewChunkLoadLimit, viewChunkBlockEntityLimit;

    // Bounding Box
    public static boolean viewCollision, viewCollisionWire, viewCollisionMask;
    public static int viewCollisionRange;

    public static void reinit() {
        var fusebox = FabricLoader.getInstance().getConfigDir().resolve("pdb.fb.properties");
        if (Files.exists(fusebox)) try (var ir = Files.newInputStream(fusebox)) {
            properties.clear();
            properties.load(ir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        defaultValue = FabricLoader.getInstance().isDevelopmentEnvironment()
                && getBoolean("default", true);

        // Anti-Xray
        viewAntiXraySet = isEnabled("viewAntiXraySet");
        viewAntiXrayUpdate = isEnabled("viewAntiXrayUpdate");
        viewAntiXrayTest = isEnabled("viewAntiXrayTest");
        viewBlockDelta = isEnabled("viewBlockDelta");
        viewBlockEvent = isEnabled("viewBlockEvent");
        viewBlockEntityUpdate = isEnabled("viewBlockEntityUpdate");
        viewChunkLoad = isEnabled("viewChunkLoad");
        viewChunkBlockEntity = isEnabled("viewChunkBlockEntity");
        viewAntiXraySetLimit = getInteger("viewAntiXraySetLimit", 2048);
        viewAntiXrayUpdateLimit = getInteger("viewAntiXrayUpdateLimit", 64);
        viewAntiXrayTestLimit = getInteger("viewAntiXrayTestLimit", 2048);
        viewBlockDeltaLimit = getInteger("viewBlockDeltaLimit", 128);
        viewBlockEventLimit = getInteger("viewBlockEventLimit", 128);
        viewBlockEntityUpdateLimit = getInteger("viewBlockEntityUpdateLimit", 128);
        viewChunkLoadLimit = getInteger("viewChunkLoadLimit", 128);
        viewChunkBlockEntityLimit = getInteger("viewChunkBlockEntityLimit", 128);

        // Bounding Box
        viewCollision = isEnabled("viewCollision");
        viewCollisionWire = getBoolean("viewCollisionWire", true);
        viewCollisionMask = viewCollisionWire && getBoolean("viewCollisionMask", true);
        viewCollisionRange = getInteger("viewCollisionRange", 16);
    }
}
