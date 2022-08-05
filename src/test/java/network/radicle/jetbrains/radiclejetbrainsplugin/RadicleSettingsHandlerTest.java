package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsHandlerTest extends LightPlatform4TestCase {
    RadicleSettingsHandler radicleSettingsHandler;

    @Before
    public void before() {
        radicleSettingsHandler = new RadicleSettingsHandler();
        radicleSettingsHandler.savePath(null);
        radicleSettingsHandler.saveRadSync(null);
    }

    @After
    public void after() {
        radicleSettingsHandler.savePath(null);
        radicleSettingsHandler.saveRadSync(null);
    }

    @Test
    public void loadSettings() {
        var settings = radicleSettingsHandler.loadSettings();
        assertThat(settings.getPath()).isNull();
        assertThat(settings.getRadSync()).isNull();
        var path = "/usr/bin/rad";
        radicleSettingsHandler.savePath(path);
        radicleSettingsHandler.saveRadSync("true");
        var newSettings = radicleSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(path);
        assertThat(newSettings.getRadSync()).isEqualTo("true");
    }


}
