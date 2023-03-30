package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RadicleSettingsHandler {

    public static final String PATH_KEY = "path";
    public static final String RAD_SEED_KEY = "radSeedKey";
    public static final String RAD_HOME = "radHome";

    public static final List<SeedNode> DEFAULT_SEED_NODES = List.of(
            new SeedNode("http://localhost:8080")
    );

    public RadicleSettingsHandler() {
        saveDefaultSeedNodes();
    }

    public RadicleSettings loadSettings() {
        return new RadicleSettings(getPath(), getSeedNodes(), getRadHome());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    public void saveRadHome(String radHome) {
        getApplicationProperties().setValue(RAD_HOME, radHome);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY, "");
    }

    private String getRadHome() {
        return getApplicationProperties().getValue(RAD_HOME, "");
    }

    public void saveSeedNodes(List<SeedNode> seedNodes) {
        String seedListToString = null;
        if (seedNodes != null) {
            seedListToString = seedNodes.stream().map(s -> s.url).collect(Collectors.joining(","));
        }
        getApplicationProperties().setValue(RAD_SEED_KEY, seedListToString);
    }

    private List<SeedNode> getSeedNodes() {
        var seeds = getApplicationProperties().getValue(RAD_SEED_KEY);
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }
        var stringNodes = seeds.split(",");
        return Arrays.stream(stringNodes).filter(s -> s.startsWith("http")).map(SeedNode::new).toList();
    }

    private void saveDefaultSeedNodes() {
        var loadedSeedNodes = getSeedNodes();
        if (loadedSeedNodes == null || loadedSeedNodes.isEmpty()) {
            saveSeedNodes(DEFAULT_SEED_NODES);
        }
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
