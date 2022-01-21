package gay.ampflower.plymouth.debug;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class Fusebox {
    private static final Properties properties = new Properties();

    static {
        var fusebox = FabricLoader.getInstance().getConfigDir().resolve("pdb.fb.properties");
        if (Files.exists(fusebox)) try (var ir = Files.newInputStream(fusebox)) {
            properties.load(ir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static final String defaultValue = Boolean.toString(fetchDefault());

    static {
        // Actually initialises the values.
        reinit();
    }

    private static boolean fetchDefault() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return false;
        String def = properties.getProperty("default");
        return def == null || Boolean.parseBoolean(def);
    }

    public static boolean isEnabled(String option) {
        return Boolean.parseBoolean(properties.getProperty(option, defaultValue));
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

    public static boolean viewAntiXraySet, viewAntiXrayUpdate, viewAntiXrayTest, viewBlockDelta, viewBlockEvent,
            viewBlockEntityUpdate, viewChunkLoad, viewChunkBlockEntity;
    public static int viewAntiXraySetLimit, viewAntiXrayUpdateLimit, viewAntiXrayTestLimit, viewBlockDeltaLimit,
            viewBlockEventLimit, viewBlockEntityUpdateLimit, viewChunkLoadLimit, viewChunkBlockEntityLimit;

    public static void reinit() {
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
    }
}
