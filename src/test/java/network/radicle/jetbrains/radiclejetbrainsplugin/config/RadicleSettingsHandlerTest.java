package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsHandlerTest extends LightPlatform4TestCase {
    RadicleSettingsHandler radicleSettingsHandler;

    @Before
    public void before() {
        radicleSettingsHandler = new RadicleSettingsHandler();
        radicleSettingsHandler.savePath(null);
    }

    @After
    public void after() {
        radicleSettingsHandler.savePath(null);
    }

    @Test
    public void loadSettings() {
        var settings = radicleSettingsHandler.loadSettings();
        var newSeedNodes = List.of(new SeedNode("http://127.0.0.1:8080"));
        assertThat(settings.getPath()).isEmpty();
        assertThat(settings.getSeedNodes()).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
        radicleSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleSettingsHandler.saveSeedNodes(newSeedNodes);
        var newSettings = radicleSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.RAD_PATH);
        assertThat(newSettings.getSeedNodes()).usingRecursiveComparison().isEqualTo(newSeedNodes);
        newSettings = radicleSettingsHandler.loadSettings();
        radicleSettingsHandler.saveSeedNodes(null);
    }

}
