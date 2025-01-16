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
        assertThat(settings.getPath()).isEmpty();
        radicleProjectSettingsHandler.savePath(AbstractIT.RAD_PATH);
        var newSettings = radicleProjectSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.RAD_PATH);
    }

    @Test
    public void testPassword() {
        radicleProjectSettingsHandler.savePassphrase(KEY, PASSWORD);
        var storedPassword = radicleProjectSettingsHandler.getPassword(KEY);
        assertThat(storedPassword).isEqualTo(PASSWORD);
    }

}
