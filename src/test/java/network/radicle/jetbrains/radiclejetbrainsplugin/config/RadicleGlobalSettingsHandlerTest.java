package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleGlobalSettingsHandlerTest extends LightPlatform4TestCase {
    RadicleGlobalSettingsHandler radicleGlobalSettingsHandler;

    @Before
    public void before() {
        radicleGlobalSettingsHandler = new RadicleGlobalSettingsHandler();
        radicleGlobalSettingsHandler.savePath(null);
    }

    @After
    public void after() {
        radicleGlobalSettingsHandler.savePath(null);
    }

    @Test
    public void loadSettings() {
        var settings = radicleGlobalSettingsHandler.loadSettings();
        var newSeedNode = "http://127.0.0.1:8080";
        assertThat(settings.getPath()).isEmpty();
        assertThat(settings.getSeedNode().url).isEqualTo(RadicleGlobalSettingsHandler.DEFAULT_SEED_NODES);
        radicleGlobalSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleGlobalSettingsHandler.saveSeedNode(newSeedNode);
        var newSettings = radicleGlobalSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.RAD_PATH);
        assertThat(newSettings.getSeedNode().url).isEqualTo(newSeedNode);
        radicleGlobalSettingsHandler.saveSeedNode(null);
    }

}
