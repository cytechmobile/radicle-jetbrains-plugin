package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.ActionsTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadStub;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsViewTest extends LightPlatform4TestCase {
    private RadicleSettingsView radicleSettingsView;
    private RadStub radStub;

    @Before
    public void before() {
        radicleSettingsView = new RadicleSettingsView();
        radicleSettingsView.apply();
        radStub = RadStub.replaceRadicleApplicationService(this);
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
        assertThat(path).isEqualTo(ActionsTest.radPath);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(ActionsTest.wsl);
            assertThat(cmd.getParametersList().get(0)).isEqualTo(".");
        } else {
            assertThat(cmd.getExePath()).isEqualTo(".");
        }
        assertThat(cmd.getCommandLineString()).contains("which rad");
    }

    @Test
    public void getRadVersion() throws InterruptedException {
        radicleSettingsView.getPathField().setText(ActionsTest.radPath);
        radicleSettingsView.apply();

        var version = radicleSettingsView.getRadVersion();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        ActionsTest.assertCmd(cmd);
        assertThat(version).isEqualTo(ActionsTest.radVersion);
        assertThat(cmd.getCommandLineString()).contains(ActionsTest.radPath + " --version");
    }

}