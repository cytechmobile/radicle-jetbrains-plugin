package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;

import java.util.Arrays;

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

    public void saveRadSync(RadicleSettings.RadSyncType type) {
        getApplicationProperties().setValue(RAD_SYNC_KEY,type.val,RadicleSettings.RadSyncType.ASK.val);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY,"");
    }

    private Integer getRadSync() {
        return getApplicationProperties().getInt(RAD_SYNC_KEY,RadicleSettings.RadSyncType.ASK.val);
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
