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
    private static final String defaultValue = Boolean.toString(FabricLoader.getInstance().isDevelopmentEnvironment());
    private static final Properties properties = new Properties();

    static {
        var fusebox = FabricLoader.getInstance().getConfigDir().resolve("pdb.fb.properties");
        if (Files.exists(fusebox)) try (var ir = Files.newInputStream(fusebox)) {
            properties.load(ir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static boolean isEnabled(String option) {
        return Boolean.parseBoolean(properties.getProperty(option, defaultValue));
    }

    public static final boolean
            VIEW_AX_SET = isEnabled("viewAntiXraySet"),
            VIEW_AX_UPDATE = isEnabled("viewAntiXrayUpdate"),
            VIEW_AX_TEST = isEnabled("viewAntiXrayTest"),
            VIEW_BLOCK_DELTA = isEnabled("viewBlockDelta"),
            VIEW_BLOCK_EVENT = isEnabled("viewBlockEvent"),
            VIEW_BLOCK_ENTITY_UPDATE = isEnabled("viewBlockEntityUpdate"),
            VIEW_CHUNK_LOAD = isEnabled("viewChunkLoad"),
            VIEW_CHUNK_BLOCK_ENTITY = isEnabled("viewChunkBlockEntity");
}
