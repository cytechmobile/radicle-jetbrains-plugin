package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        var newSeedNodes = List.of(new SeedNode("192.168.1.1", "8080"));
        assertThat(settings.getPath()).isEmpty();
        assertThat(settings.getSeedNodes()).usingRecursiveComparison().isEqualTo(RadicleGlobalSettingsHandler.DEFAULT_SEED_NODES);
        radicleGlobalSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleGlobalSettingsHandler.saveSeedNodes(newSeedNodes);
        var newSettings = radicleGlobalSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.RAD_PATH);
        assertThat(newSettings.getSeedNodes()).usingRecursiveComparison().isEqualTo(newSeedNodes);
        radicleGlobalSettingsHandler.saveSeedNodes(null);
    }

}
