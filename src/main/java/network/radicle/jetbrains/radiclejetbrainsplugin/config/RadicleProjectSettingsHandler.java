package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;

public class RadicleProjectSettingsHandler {

    public static final String RAD_HOME_KEY = "radHome";
    private final Project project;

    public RadicleProjectSettingsHandler(Project project) {
        this.project = project;
    }

    public String getRadHome() {
        return getApplicationProperties().getValue(RAD_HOME_KEY, "");
    }

    public void saveRadHome(String radHome) {
        getApplicationProperties().setValue(RAD_HOME_KEY, radHome, "");
    }

    public RadicleProjectSettings loadSettings() {
        return new RadicleProjectSettings(getRadHome());
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance(project);
    }

}
