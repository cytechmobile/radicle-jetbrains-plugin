package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.ide.util.PropertiesComponent;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;


public class RadicleGlobalSettingsHandler {

    public static final String PATH_KEY = "path";
    public static final String RAD_SEED_KEY = "radSeedKey";
    public static final String DEFAULT_SEED_NODES = "http://localhost:8080";

    public RadicleGlobalSettingsHandler() {
        saveDefaultSeedNodes();
    }

    public RadicleGlobalSettings loadSettings() {
        return new RadicleGlobalSettings(getPath(), getSeedNode());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY, "");
    }


    public void saveSeedNode(String seedNode) {
        getApplicationProperties().setValue(RAD_SEED_KEY, Strings.nullToEmpty(seedNode));
    }

    private SeedNode getSeedNode() {
        var seed = getApplicationProperties().getValue(RAD_SEED_KEY);
        if (Strings.isNullOrEmpty(seed)) {
            return new SeedNode("");
        }
        return new SeedNode(seed);
    }

    private void saveDefaultSeedNodes() {
        var loadedSeedNode = getSeedNode();
        if (Strings.isNullOrEmpty(loadedSeedNode.url)) {
            saveSeedNode(DEFAULT_SEED_NODES);
        }
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
