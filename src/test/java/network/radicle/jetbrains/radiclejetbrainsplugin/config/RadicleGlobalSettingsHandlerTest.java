package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleGlobalSettingsHandlerTest extends LightPlatform4TestCase {
    private static final String KEY = "myKey";
    public static final String PASSWORD = "password";
    RadicleProjectSettingsHandler radicleProjectSettingsHandler;

    @Before
    public void before() {
        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleProjectSettingsHandler.savePath(null);
    }

    @After
    public void after() {
        radicleProjectSettingsHandler.savePath(null);
    }

    @Test
    public void loadSettings() {
        var settings = radicleProjectSettingsHandler.loadSettings();
        var newSeedNode = "http://127.0.0.1:8080";
        assertThat(settings.getPath()).isEmpty();
        assertThat(settings.getSeedNode().url).isEqualTo(RadicleProjectSettingsHandler.DEFAULT_SEED_NODES);
        radicleProjectSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleProjectSettingsHandler.saveSeedNode(newSeedNode);
        var newSettings = radicleProjectSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.RAD_PATH);
        assertThat(newSettings.getSeedNode().url).isEqualTo(newSeedNode);
        radicleProjectSettingsHandler.saveSeedNode(null);
    }

    @Test
    public void testPassword() {
        radicleProjectSettingsHandler.savePassphrase(KEY, PASSWORD);
        var storedPassword = radicleProjectSettingsHandler.getPassword(KEY);
        assertThat(storedPassword).isEqualTo(PASSWORD);
    }

}
