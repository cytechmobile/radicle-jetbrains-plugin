package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;

public class RadiclePathHandler {

    static final String PATH_KEY = "path";

    public RadiclePathHandler() {
    }

    public RadiclePath loadPath() {
        return new RadiclePath(getPath());
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
