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

    public static final boolean
            VIEW_AX_SET = Boolean.parseBoolean(properties.getProperty("viewAntiXraySet", "true")),
            VIEW_AX_UPDATE = Boolean.parseBoolean(properties.getProperty("viewAntiXrayUpdate", "true")),
            VIEW_AX_TEST = Boolean.parseBoolean(properties.getProperty("viewAntiXrayTest", "true")),
            VIEW_BLOCK_DELTA = Boolean.parseBoolean(properties.getProperty("viewBlockDelta", "true")),
            VIEW_BLOCK_EVENT = Boolean.parseBoolean(properties.getProperty("viewBlockEvent", "true")),
            VIEW_BLOCK_ENTITY_UPDATE = Boolean.parseBoolean(properties.getProperty("viewBlockEntityUpdate", "true")),
            VIEW_CHUNK_LOAD = Boolean.parseBoolean(properties.getProperty("viewChunkLoad", "true")),
            VIEW_CHUNK_BLOCK_ENTITY = Boolean.parseBoolean(properties.getProperty("viewChunkBlockEntity", "true"));
}
