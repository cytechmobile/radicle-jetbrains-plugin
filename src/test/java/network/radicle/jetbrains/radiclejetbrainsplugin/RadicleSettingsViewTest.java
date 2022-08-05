package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import org.junit.Before;
import org.junit.Test;

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

}
