package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
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
        radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.ASK);
    }

    @After
    public void after() {
        radicleSettingsHandler.savePath(null);
        radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.ASK);
    }

    @Test
    public void loadSettings() {
        var settings = radicleSettingsHandler.loadSettings();
        var newSeedNodes = List.of(new SeedNode("192.168.1.1","8080"));
        assertThat(settings.getPath()).isEmpty();
        assertThat(settings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.ASK.val);
        assertThat(settings.getSeedNodes()).usingRecursiveComparison().isEqualTo(RadicleSettingsHandler.DEFAULT_SEED_NODES);
        radicleSettingsHandler.savePath(AbstractIT.radPath);
        radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.YES);
        radicleSettingsHandler.saveSeedNodes(newSeedNodes);
        var newSettings = radicleSettingsHandler.loadSettings();
        assertThat(newSettings.getPath()).isEqualTo(AbstractIT.radPath);
        assertThat(newSettings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.YES.val);
        assertThat(newSettings.getSeedNodes()).usingRecursiveComparison().isEqualTo(newSeedNodes);
        radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.NO);
        newSettings = radicleSettingsHandler.loadSettings();
        assertThat(newSettings.getRadSync()).isEqualTo(RadicleSettings.RadSyncType.NO.val);
        radicleSettingsHandler.saveSeedNodes(null);
    }

}
