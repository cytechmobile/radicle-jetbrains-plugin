package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

import java.util.List;
import java.util.stream.Collectors;

public class RadicleGlobalSettingsHandler {

    public static final String PATH_KEY = "path";
    public static final String RAD_SEED_KEY = "radSeedKey";

    public static final String RAD_SEED_SEPERATOR = "|";
    public static final String DEFAULT_SEED_PORT = "8777";

    public static final String PINE_DOMAIN_NAME = "pine.radicle.garden";
    public static final String WILLOW_DOMAIN_NAME = "willow.radicle.garden";
    public static final String MAPLE_DOMAIN_NAME = "maple.radicle.garden";

    public static final List<SeedNode> DEFAULT_SEED_NODES = List.of(
            new SeedNode(PINE_DOMAIN_NAME, DEFAULT_SEED_PORT),
            new SeedNode(WILLOW_DOMAIN_NAME, DEFAULT_SEED_PORT),
            new SeedNode(MAPLE_DOMAIN_NAME, DEFAULT_SEED_PORT)
    );

    public RadicleGlobalSettingsHandler() {
        saveDefaultSeedNodes();
    }

    public RadicleGlobalSettings loadSettings() {
        return new RadicleGlobalSettings(getPath(), getSeedNodes());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY, "");
    }


    public void saveSeedNodes(List<SeedNode> seedNodes) {
        String seedListToString = null;
        if (seedNodes != null) {
            seedListToString = seedNodes.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        getApplicationProperties().setValue(RAD_SEED_KEY, seedListToString);
    }

    private List<SeedNode> getSeedNodes() {
        var seeds = getApplicationProperties().getValue(RAD_SEED_KEY);
        if (seeds != null) {
            if (seeds.isEmpty()) {
                return List.of();
            } else {
                var stringNodes = seeds.split(",");
                return SeedNode.getNodesFromString(stringNodes);
            }
        }
        return null;
    }

    private void saveDefaultSeedNodes() {
        var loadedSeedNodes = getSeedNodes();
        if (loadedSeedNodes == null) {
            saveSeedNodes(DEFAULT_SEED_NODES);
        }
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
