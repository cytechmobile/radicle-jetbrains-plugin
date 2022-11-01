package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadStub;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsViewTest extends LightPlatform4TestCase {
    private RadicleSettingsView radicleSettingsView;
    private RadicleSettingsHandler radicleSettingsHandler;
    private RadStub radStub;

    @Before
    public void before() {
        radStub = RadStub.replaceRadicleApplicationService(this);
        radicleSettingsHandler = new RadicleSettingsHandler();
        radicleSettingsView = new RadicleSettingsView();
        radicleSettingsView.apply();
    }

    @Test
    public void testGetId() {
      assertThat(radicleSettingsView.getId()).isEqualTo(RadicleSettingsView.ID);
    }

    @Test
    public void testDisplayName() {
        assertThat(radicleSettingsView.getDisplayName()).isEqualTo(RadicleSettingsView.ID);
    }

    @Test
    public void testCreateComponent() {
        assertThat(radicleSettingsView.createComponent()).isNotNull();
    }

    @Test
    public void testIsModifiedApply() throws ConfigurationException, InterruptedException {
        assertThat(radicleSettingsView.isModified()).isFalse();
        radicleSettingsView.getPathField().setText("/radpath");
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();

        radicleSettingsView = new RadicleSettingsView();
        assertThat(radicleSettingsView.isModified()).isFalse();
        assertThat(radicleSettingsView.getComboBox().getSelectedIndex()).isEqualTo(RadicleSettings.RadSyncType.ASK.val);

        radicleSettingsView.getComboBox().setSelectedIndex(RadicleSettings.RadSyncType.YES.val);
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();

        radicleSettingsView = new RadicleSettingsView();
        assertThat(radicleSettingsView.isModified()).isFalse();
        assertThat(radicleSettingsView.getComboBox().getSelectedIndex()).isEqualTo(RadicleSettings.RadSyncType.YES.val);
    }

    @Test
    public void testDisposeUIResources() {
        radicleSettingsView.disposeUIResources();
        assertThat(radicleSettingsView.getMainPanel()).isNull();
        assertThat(radicleSettingsView.getPathField()).isNull();
        assertThat(radicleSettingsView.getTestButton()).isNull();
        assertThat(radicleSettingsView.getRadVersionLabel()).isNull();
    }

    @Test
    public void getRadPathTest() throws InterruptedException {
        var path = radicleSettingsView.getRadPath();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(path).isEqualTo(AbstractIT.radPath);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(AbstractIT.wsl);
            assertThat(cmd.getParametersList().get(0)).isEqualTo("bash");
            assertThat(cmd.getParametersList().get(1)).isEqualTo("-ic");
        } else {
            assertThat(cmd.getExePath()).isEqualTo("which");
        }
        assertThat(cmd.getCommandLineString()).contains("which rad");
    }

    @Test
    public void getRadVersion() throws InterruptedException {
        radicleSettingsHandler.savePath(AbstractIT.radPath);
        /* pop previous command from queue ( placeholder triggers it ) */
        radStub.commands.poll(10, TimeUnit.SECONDS);
        radicleSettingsView = new RadicleSettingsView();
        var version = radicleSettingsView.getRadVersion();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        AbstractIT.assertCmd(cmd);
        assertThat(version).isEqualTo(AbstractIT.radVersion);
        assertThat(cmd.getCommandLineString()).contains(AbstractIT.radPath + " --version");
    }

}
