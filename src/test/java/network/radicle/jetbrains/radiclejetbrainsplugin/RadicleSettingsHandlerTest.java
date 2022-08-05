package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.testFramework.LightPlatform4TestCase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.tests.JUnit5Runner;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;


public class RadicleSettingsHandlerTest extends LightPlatform4TestCase {
    RadicleSettingsHandler radicleSettingsHandler;

    @Before
    public void before() {
        radicleSettingsHandler = new RadicleSettingsHandler();
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
