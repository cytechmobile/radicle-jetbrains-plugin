package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.ActionsTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadStub;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsViewTest extends LightPlatform4TestCase {

    private RadicleSettingsView radicleSettingsView;
    RadStub radStub;

    @Before
    public void before() throws ConfigurationException {
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
    public void testIsModifiedApply() throws ConfigurationException {
        assertThat(radicleSettingsView.isModified()).isFalse();
        radicleSettingsView.getPathField().setText("/usr/bin/rad");
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();
        assertThat(radicleSettingsView.isModified()).isFalse();
        assertThat(radicleSettingsView.getPathField().getText()).isEqualTo("/usr/bin/rad");

        assertThat(radicleSettingsView.getRadSyncCheckBox().isSelected()).isFalse();
        radicleSettingsView.getRadSyncCheckBox().setSelected(true);
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();
        assertThat(radicleSettingsView.isModified()).isFalse();
        assertThat(radicleSettingsView.getRadSyncCheckBox().isSelected()).isTrue();
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
        radicleSettingsView.getPathField().setText(ActionsTest.radPath);
        radicleSettingsView.apply();

        var path = radicleSettingsView.getRadPath();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(path).isEqualTo(ActionsTest.radPath);
        assertThat(cmd).isNotNull();
        assertThat(cmd.getExePath()).isEqualTo(ActionsTest.wsl);
        assertThat(cmd.getCommandLineString()).contains("which rad");
    }

    @Test
    public void getRadVersion() throws InterruptedException {
        radicleSettingsView.getPathField().setText(ActionsTest.radPath);
        radicleSettingsView.apply();

        var version = radicleSettingsView.getRadVersion();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertThat(version).isEqualTo(ActionsTest.radVersion);
        assertThat(cmd.getExePath()).isEqualTo(ActionsTest.wsl);
        assertThat(cmd.getCommandLineString()).contains(ActionsTest.radPath + " --version");
    }

}
