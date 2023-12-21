package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;

public class RadicleProjectSettingsHandler {
    public static final String RAD_SETTINGS_PREFIX = RadicleProjectSettingsHandler.class.getPackageName();
    public static final String RAD_HOME_KEY = RAD_SETTINGS_PREFIX + ".radHome";
    public static final String PATH_KEY = RAD_SETTINGS_PREFIX + ".path";
    public static final String RAD_SEED_KEY = RAD_SETTINGS_PREFIX + ".radSeedKey";
    public static final String DEFAULT_SEED_NODES = "http://localhost:8080";
    private final Project project;

    public RadicleProjectSettingsHandler(Project project) {
        this.project = project;
        saveDefaultSeedNodes();
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

    private SeedNode getSeedNode() {
        var seed = getApplicationProperties().getValue(RAD_SEED_KEY);
        if (Strings.isNullOrEmpty(seed)) {
            return new SeedNode("");
        }
        return new SeedNode(seed);
    }

    public void saveRadHome(String radHome) {
        getApplicationProperties().setValue(RAD_HOME_KEY, radHome, "");
    }

    public void savePath(String path) {
        getApplicationProperties().setValue(PATH_KEY, path);
    }

    public void saveSeedNode(String seedNode) {
        getApplicationProperties().setValue(RAD_SEED_KEY, Strings.nullToEmpty(seedNode));
    }

    public void savePassphrase(String key, String passphrase) {
        storeCredentials(key, passphrase);
    }

    private void saveDefaultSeedNodes() {
        var loadedSeedNode = getSeedNode();
        if (Strings.isNullOrEmpty(loadedSeedNode.url)) {
            saveSeedNode(DEFAULT_SEED_NODES);
        }
    }

    public RadicleProjectSettings loadSettings() {
        return new RadicleProjectSettings(getRadHome(), getPath(), getSeedNode());
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
