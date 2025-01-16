package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;

public class RadicleProjectSettingsHandler {
    public static final String RAD_SETTINGS_PREFIX = RadicleProjectSettingsHandler.class.getPackageName();
    public static final String RAD_HOME_KEY = RAD_SETTINGS_PREFIX + ".radHome";
    public static final String PATH_KEY = RAD_SETTINGS_PREFIX + ".path";
    private final Project project;

    public RadicleProjectSettingsHandler(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public String getRadHome() {
        return getApplicationProperties().getValue(RAD_HOME_KEY, "");
    }

    public String getPath() {
        return getApplicationProperties().getValue(PATH_KEY, "");
    }

    public String getPassword(String key) {
        return retrieveStoredCredentials(key);
    }

    public boolean isSettingsEmpty() {
        return Strings.isNullOrEmpty(getPath()) || Strings.isNullOrEmpty(getRadHome());
    }

    public void saveRadHome(String radHome) {
        getApplicationProperties().setValue(RAD_HOME_KEY, radHome, "");
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    public void savePassphrase(String key, String passphrase) {
        storeCredentials(key, passphrase);
    }

    public RadicleProjectSettings loadSettings() {
        return new RadicleProjectSettings(getRadHome(), getPath());
    }

    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance(project);
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName("MySystem", key));
    }

    private void storeCredentials(String key, String password) {
        var credentialAttributes = createCredentialAttributes(key);
        var credentials = new Credentials(key, password);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    private String retrieveStoredCredentials(String key) {
        var credentialAttributes = createCredentialAttributes(key);
        var credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials != null) {
            return credentials.getPasswordAsString();
        }
        return null;
    }

}
