package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.LightPlatform4TestCase;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadStub;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT.assertCmd;
import static org.assertj.core.api.Assertions.assertThat;

public class RadicleSettingsViewTest extends LightPlatform4TestCase {
    private static final Logger logger = LoggerFactory.getLogger(RadicleSettingsViewTest.class);

    private RadicleSettingsView radicleSettingsView;
    private RadicleProjectSettingsHandler radicleProjectSettingsHandler;
    private RadicleProjectSettingsHandler radicleSettingsHandler;
    private RadStub radStub;

    @Before
    public void before() throws InterruptedException {
        radStub = RadStub.replaceRadicleProjectService(this, "", getProject());
        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        radicleSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleSettingsHandler.saveSeedNode("http://localhost:8080");
        radicleSettingsView = new RadicleSettingsView(getProject());
        boolean ok = radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        /* pop previous commands from queue ( Checking for compatible version ) */
        radStub.commands.clear();
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
    public void testIsModifiedApply() {
        var identityDialog = new IdentityDialog() {
            @Override
            public boolean showAndGet() {
                return true;
            }
        };
        radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        assertThat(radicleSettingsView.isModified()).isFalse();
        radicleSettingsView.getPathField().setText("/radpath");
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();

        radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        radicleSettingsView.getHomeField().setText("/home");
        assertThat(radicleSettingsView.isModified()).isTrue();
        radicleSettingsView.apply();
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
    public void testButtonWithUnlockedIdentity() throws InterruptedException {
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        radicleSettingsView = new RadicleSettingsView(getProject());
        radStub.commands.poll(10, TimeUnit.SECONDS);
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        var radSelfCmd =  radStub.commands.poll(10, TimeUnit.SECONDS);
        var identityUnlockedCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCommands(radSelfCmd, identityUnlockedCmd, AbstractIT.RAD_HOME);
        for (int i = 0; i < 10; i++) {
            if (radicleSettingsView.getRadDetails() == null) {
                Thread.sleep(100);
            }
        }
        logger.warn("checking for rad details: " + radicleSettingsView.getRadDetails());
        assertThat(radicleSettingsView.getRadDetails().did).isEqualTo(RadStub.did);
    }

    @Test
    public void testButtonWithoutIdentity() throws InterruptedException {
        var identityDialog = new IdentityDialog() {
            @Override
            public boolean showAndGet() {
                assertThat(getTitle()).isEqualTo(RadicleBundle.message("newIdentity"));
                return true;
            }
        };
        radicleProjectSettingsHandler.saveRadHome("/lakis");
        radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        radStub.commands.poll(10, TimeUnit.SECONDS);
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        var radSelfCmd =  radStub.commands.poll(10, TimeUnit.SECONDS);
        var identityUnlockedCmd = radStub.commands.poll(100, TimeUnit.MILLISECONDS);
        assertThat(radSelfCmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(radSelfCmd.getCommandLineString()).contains("export RAD_HOME=/lakis");
        } else {
            assertThat(radSelfCmd.getEnvironment().get("RAD_HOME")).contains("lakis");
        }
        assertThat(radSelfCmd.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfCmd.getCommandLineString()).contains("rad self");
        assertThat(identityUnlockedCmd).isNull();
    }

    @Test
    public void testButtonWithLockedIdentity() throws InterruptedException {
        var identityDialog = new IdentityDialog() {
            @Override
            public boolean showAndGet() {
                assertThat(getTitle()).isEqualTo(RadicleBundle.message("unlockIdentity"));
                return true;
            }
        };
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME1);
        radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        radStub.commands.poll(10, TimeUnit.SECONDS);
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        var radSelfCmd =  radStub.commands.poll(10, TimeUnit.SECONDS);
        var identityUnlockedCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCommands(radSelfCmd, identityUnlockedCmd, AbstractIT.RAD_HOME1);
    }

    @Test
    public void testButtonWithStoredPassword() throws InterruptedException {
        radicleProjectSettingsHandler.savePassphrase(RadStub.nodeId, RadicleGlobalSettingsHandlerTest.PASSWORD);
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME1);
        radicleSettingsView = new RadicleSettingsView(getProject());
        radStub.commands.poll(10, TimeUnit.SECONDS);
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        var radSelfCmd =  radStub.commands.poll(10, TimeUnit.SECONDS);
        var identityUnlockedCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCommands(radSelfCmd, identityUnlockedCmd, AbstractIT.RAD_HOME1);
        var auth = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(auth.getCommandLineString()).contains("rad auth --stdin");
        assertThat(auth.getEnvironment().get("stdin")).isEqualTo(RadicleGlobalSettingsHandlerTest.PASSWORD);
    }

    @Test
    public void getRadHomeTest() throws InterruptedException {
        var home = radicleSettingsView.getRadHome();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(home).isEqualTo(AbstractIT.RAD_HOME);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("rad path");
    }

    @Test
    public void getRadPathTest() throws InterruptedException {
        var path = radicleSettingsView.getRadPath();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(path).isEqualTo(AbstractIT.RAD_PATH);
        assertThat(cmd).isNotNull();
        assertThat(cmd.getCommandLineString()).contains("which rad");
    }

    @Test
    public void getRadVersion() throws InterruptedException {
        var version = radicleSettingsView.getRadVersion();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(version).isEqualTo(AbstractIT.RAD_VERSION);
        assertThat(cmd.getCommandLineString()).contains(AbstractIT.RAD_PATH + " --version");
    }

    private void assertCommands(GeneralCommandLine radSelfCmd, GeneralCommandLine identityUnlockedCmd, String radHome) {
        assertThat(radSelfCmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(radSelfCmd.getCommandLineString()).contains("export RAD_HOME=" + radHome);
        } else {
            assertThat(radSelfCmd.getEnvironment().get("RAD_HOME")).contains(radHome);
        }
        assertThat(radSelfCmd.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfCmd.getCommandLineString()).contains("rad self");
        assertThat(identityUnlockedCmd).isNotNull();
        assertThat(identityUnlockedCmd.getCommandLineString()).contains("ssh-add -l");
    }
}
