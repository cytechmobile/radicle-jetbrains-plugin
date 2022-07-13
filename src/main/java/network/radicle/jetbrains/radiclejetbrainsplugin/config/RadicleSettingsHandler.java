package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;

public class RadicleSettingsHandler {

    static final String PATH_KEY = "path";

    public RadicleSettingsHandler() {
    }

    public RadicleSettings loadSettings() {
        return new RadicleSettings(getPath());
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    private String getPath() {
        return getApplicationProperties().getValue(PATH_KEY);
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
