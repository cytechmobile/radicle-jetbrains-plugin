package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;

import java.util.List;

public class RadicleSettingsHandler {

    static final String PATH_KEY = "path";
    static final String RAD_SYNC_KEY = "radSyncKey";
    static final String RAD_SEED_KEY = "radSeedKey";

    public static final String RAD_SEED_SEPERATOR = "|";
    public static final String DEFAULT_SEED_PORT = "8777";
    static final String PINE_DOMAIN_NAME = "pine.radicle.garden";
    static final String WILLOW_DOMAIN_NAME = "willow.radicle.garden";
    static final String MAPLE_DOMAIN_NAME = "maple.radicle.garden";

    static final List<String> DEFAULT_SEED_NODES = List.of(
            PINE_DOMAIN_NAME + RAD_SEED_SEPERATOR + DEFAULT_SEED_PORT,
            WILLOW_DOMAIN_NAME + RAD_SEED_SEPERATOR + DEFAULT_SEED_PORT,
            MAPLE_DOMAIN_NAME + RAD_SEED_SEPERATOR + DEFAULT_SEED_PORT
    );

    public RadicleSettingsHandler() {
        saveDefaultSeedNodes();
    }

    public RadicleSettings loadSettings() {
        return new RadicleSettings(getPath(),getRadSync(),getSeedNodes());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    public void saveRadSync(RadicleSettings.RadSyncType type) {
        getApplicationProperties().setValue(RAD_SYNC_KEY,type.val,RadicleSettings.RadSyncType.ASK.val);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY,"");
    }

    private Integer getRadSync() {
        return getApplicationProperties().getInt(RAD_SYNC_KEY,RadicleSettings.RadSyncType.ASK.val);
    }

    public void saveSeedNodes(List<String> seedNodes) {
        getApplicationProperties().setList(RAD_SEED_KEY, seedNodes);
    }

    private List<String> getSeedNodes() {
        return getApplicationProperties().getList(RAD_SEED_KEY);
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
