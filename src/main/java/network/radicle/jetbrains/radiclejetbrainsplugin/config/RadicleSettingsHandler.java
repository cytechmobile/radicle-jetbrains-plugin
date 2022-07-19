package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;

public class RadicleSettingsHandler {

    static final String PATH_KEY = "path";
    static final String RAD_SYNC_KEY = "radSyncKey";

    public RadicleSettingsHandler() {
    }

    public RadicleSettings loadSettings() {
        return new RadicleSettings(getPath(),getRadSync());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    public void saveRadSync(String radSync) {
        getApplicationProperties().setValue(RAD_SYNC_KEY, radSync);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY);
    }

    private String getRadSync() {
        return getApplicationProperties().getValue(RAD_SYNC_KEY);
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
