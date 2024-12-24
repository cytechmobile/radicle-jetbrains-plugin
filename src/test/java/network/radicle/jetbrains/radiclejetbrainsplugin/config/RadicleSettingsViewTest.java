package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.CoroutineKt;
import com.intellij.testFramework.LightPlatform4TestCase;
import com.intellij.testFramework.PlatformTestUtil;
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
    private static final String ALIAS = "myAlias";
    private static final Logger logger = LoggerFactory.getLogger(RadicleSettingsViewTest.class);
    public static final String NEW_RAD_INSTALLATION = "/newRadInstallation";
    private RadicleProjectSettingsHandler radicleSettingsHandler;
    private RadStub radStub;

    @Before
    public void before() throws InterruptedException {
        radStub = RadStub.replaceRadicleProjectService(this, "", getProject());
        radicleSettingsHandler = new RadicleProjectSettingsHandler(getProject());
        radicleSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        radicleSettingsHandler.savePath(AbstractIT.RAD_PATH);
        radicleSettingsHandler.saveSeedNode("http://localhost:8080");
        radStub.commands.clear();
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
        identityDialog.setPassphrase(RadicleGlobalSettingsHandlerTest.PASSWORD);
        identityDialog.setAlias(ALIAS);
        radicleSettingsHandler.saveRadHome(NEW_RAD_INSTALLATION);
        var radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        // wait to retrieve and remove the `rad --version` command
        radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        radStub.commands.clear();
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        executeUiTasks();
        radicleSettingsView.getLatch().await(20, TimeUnit.SECONDS);
        assertSelfCommands(NEW_RAD_INSTALLATION);
        var alias = pollCmd();
        assertThat(alias.getCommandLineString()).contains("rad auth --stdin --alias " + ALIAS);
    }

    @Test
    public void testGetId() {
        var radicleSettingsView = new RadicleSettingsView(getProject());
        assertThat(radicleSettingsView.getId()).isEqualTo(RadicleSettingsView.ID);
    }

    @Test
    public void testDisplayName() {
        var radicleSettingsView = new RadicleSettingsView(getProject());
        assertThat(radicleSettingsView.getDisplayName()).isEqualTo(RadicleSettingsView.ID);
    }

    @Test
    public void testCreateComponent() {
        var radicleSettingsView = new RadicleSettingsView(getProject());
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
        var radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
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
        var radicleSettingsView = new RadicleSettingsView(getProject());
        radicleSettingsView.disposeUIResources();
        assertThat(radicleSettingsView.getMainPanel()).isNull();
        assertThat(radicleSettingsView.getPathField()).isNull();
        assertThat(radicleSettingsView.getTestButton()).isNull();
        assertThat(radicleSettingsView.getRadVersionLabel()).isNull();
    }

    @Test
    public void testButtonWithUnlockedIdentity() throws InterruptedException {
        radicleSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        var radicleSettingsView = new RadicleSettingsView(getProject());
        radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        radStub.commands.clear();
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        radicleSettingsView.getLatch().await(20, TimeUnit.SECONDS);
        assertSelfCommands(AbstractIT.RAD_HOME);
        executeUiTasks();
        for (int i = 0; i < 10; i++) {
            if (radicleSettingsView.getRadDetails() == null) {
                Thread.sleep(100);
            }
        }
        assertThat(radicleSettingsView.getRadDetails().did).isEqualTo(RadStub.SELF_DID);
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
        identityDialog.setPassphrase(RadicleGlobalSettingsHandlerTest.PASSWORD);
        radicleSettingsHandler.saveRadHome(AbstractIT.RAD_HOME1);
        var radicleSettingsView = new RadicleSettingsView(identityDialog, getProject());
        radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        radStub.commands.clear();
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        radicleSettingsView.getLatch().await(20, TimeUnit.SECONDS);
        executeUiTasks();
        assertSelfCommands(AbstractIT.RAD_HOME1);
        //Remove ssh
        pollCmd();
        var auth = pollCmd();
        assertThat(auth.getCommandLineString()).contains("rad auth --stdin");
        assertThat(auth.getCommandLineString()).doesNotContain("--alias");
    }

    @Test
    public void testButtonWithStoredEmptyPassword() throws InterruptedException {
        radicleSettingsHandler.savePassphrase(RadStub.SELF_NODEID, "");
        radicleSettingsHandler.saveRadHome(AbstractIT.RAD_HOME1);
        var radicleSettingsView = new RadicleSettingsView(getProject());
        radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        pollCmd();
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        radicleSettingsView.getLatch().await(20, TimeUnit.SECONDS);
        executeUiTasks();
        assertSelfCommands(AbstractIT.RAD_HOME1);
        //remove ssh
        pollCmd();
        var auth = pollCmd();
        assertThat(auth.getCommandLineString()).contains("rad auth --stdin");
        assertThat(auth.getEnvironment().get("stdin")).isEqualTo("");
    }

    @Test
    public void testButtonWithStoredPassword() throws InterruptedException {
        radicleSettingsHandler.savePassphrase(RadStub.SELF_NODEID, RadicleGlobalSettingsHandlerTest.PASSWORD);
        radicleSettingsHandler.saveRadHome(AbstractIT.RAD_HOME1);
        var radicleSettingsView = new RadicleSettingsView(getProject());
        radicleSettingsView.init.await(10, TimeUnit.SECONDS);
        radStub.commands.clear();
        var testButton = radicleSettingsView.getRadHomeTestButton();
        testButton.doClick();
        radicleSettingsView.getLatch().await(20, TimeUnit.SECONDS);
        assertSelfCommands(AbstractIT.RAD_HOME1);
        //Remove ssh
        pollCmd();
        var auth = pollCmd();
        assertThat(auth.getCommandLineString()).contains("rad auth --stdin");
        assertThat(auth.getEnvironment().get("stdin")).isEqualTo(RadicleGlobalSettingsHandlerTest.PASSWORD);
    }

   @Test
   public void getRadHomeTest() throws InterruptedException {
       radicleSettingsHandler.saveRadHome("");
       var radicleSettingsView = new RadicleSettingsView(getProject());
       radicleSettingsView.init.await(30, TimeUnit.SECONDS);
       radStub.commands.clear();
       var home = radicleSettingsView.getRadHome();
       var cmd = pollCmd();
       assertThat(home).isEqualTo(AbstractIT.RAD_HOME);
       assertThat(cmd).isNotNull();
       assertCmd(cmd);
       assertThat(cmd.getCommandLineString()).contains("rad path");
   }

  @Test
  public void getRadPathTest() throws InterruptedException {
      var radicleSettingsView = new RadicleSettingsView(getProject());
      radicleSettingsView.init.await(30, TimeUnit.SECONDS);
      radStub.commands.clear();
      var path = radicleSettingsView.getRadPath();
      var cmd = pollCmd();
      assertThat(path).isEqualTo(AbstractIT.RAD_PATH);
      assertThat(cmd).isNotNull();
      assertThat(cmd.getCommandLineString()).contains("which rad");
  }

  @Test
  public void getRadVersion() throws InterruptedException {
      var radicleSettingsView = new RadicleSettingsView(getProject());
      radicleSettingsView.init.await(30, TimeUnit.SECONDS);
      radStub.commands.clear();
      var version = radicleSettingsView.getRadVersion();
      var cmd = pollCmd();
      assertCmd(cmd);
      assertThat(version).isEqualTo(AbstractIT.RAD_VERSION);
      assertThat(cmd.getCommandLineString()).contains(AbstractIT.RAD_PATH + " --version");
  }

    private void assertSelfCommands(String radHome) throws InterruptedException {
        var radSelfAlias = pollCmd();
        var radSelfNid = pollCmd();
        var radSelfDid = pollCmd();
        var radSelfKey = pollCmd();
        assertThat(radSelfAlias).isNotNull();
        assertThat(radSelfNid).isNotNull();
        assertThat(radSelfDid).isNotNull();
        assertThat(radSelfKey).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(radSelfAlias.getCommandLineString()).contains("export RAD_HOME=" + radHome);
            assertThat(radSelfNid.getCommandLineString()).contains("export RAD_HOME=" + radHome);
            assertThat(radSelfDid.getCommandLineString()).contains("export RAD_HOME=" + radHome);
            assertThat(radSelfKey.getCommandLineString()).contains("export RAD_HOME=" + radHome);
        } else {
            assertThat(radSelfAlias.getEnvironment().get("RAD_HOME")).contains(radHome);
            assertThat(radSelfNid.getEnvironment().get("RAD_HOME")).contains(radHome);
            assertThat(radSelfDid.getEnvironment().get("RAD_HOME")).contains(radHome);
            assertThat(radSelfKey.getEnvironment().get("RAD_HOME")).contains(radHome);
        }
        assertThat(radSelfAlias.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfAlias.getCommandLineString()).contains("rad self --alias");
        assertThat(radSelfNid.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfNid.getCommandLineString()).contains("rad self --nid");
        assertThat(radSelfDid.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfDid.getCommandLineString()).contains("rad self --did");
        assertThat(radSelfKey.getCommandLineString()).contains(AbstractIT.RAD_PATH);
        assertThat(radSelfKey.getCommandLineString()).contains("rad self --ssh-fingerprint");
    }

    public void executeUiTasks() {
        for (int i = 0; i < 10; i++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            CoroutineKt.executeSomeCoroutineTasksAndDispatchAllInvocationEvents(getProject());
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            Thread.yield();
        }
    }

    public GeneralCommandLine pollCmd() throws InterruptedException {
        return radStub.commands.poll(10, TimeUnit.SECONDS);
    }
}
