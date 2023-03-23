package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.util.SystemInfo;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePullAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsIdentitiesView;
import network.radicle.jetbrains.radiclejetbrainsplugin.listeners.RadicleManagerListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ActionsTest extends AbstractIT {

    @Test
    public void cloneTest() throws InterruptedException {
        var radUrl = "rad:git:123";
        var clone = new RadClone(radUrl, "C:\\");
        clone.perform();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(WSL);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(RAD_PATH);
        }
        assertThat(cmd.getCommandLineString()).contains("clone " + radUrl);
    }

    @Test
    public void getActiveProfileTest() throws InterruptedException {
        var identitiesView = new RadicleSettingsIdentitiesView();
        identitiesView.getActiveProfile();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        if (SystemInfo.isWindows) {
            assertThat(cmd.getExePath()).isEqualTo(WSL);
        } else {
            assertThat(cmd.getExePath()).isEqualTo(RAD_PATH);
        }
        assertThat(cmd.getCommandLineString()).contains("self --profile");
    }

    @Test
    public void setDefaultProfileTest() throws InterruptedException {
        var identityToBeDefault = "test";
        var identitiesView = new RadicleSettingsIdentitiesView();
        RadicleSettingsIdentitiesView.DefaultProfileButton defaultIdentity = identitiesView.new DefaultProfileButton();
        defaultIdentity.setDefaultProfile(identityToBeDefault);
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("rad auth " + identityToBeDefault);

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radNotification_AuthSetDefaultIdentity"));
    }

    @Test
    public void removeIdentityTest() throws InterruptedException {
        var identityToDelete = "test";
        var identitiesView = new RadicleSettingsIdentitiesView();
        RadicleSettingsIdentitiesView.RemoveProfileButton removeIdentity = identitiesView.new RemoveProfileButton();
        removeIdentity.removeProfile(identityToDelete);

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("rad self");

        cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        var gitStoragePath = RadStub.GIT_STORAGE_PATH + "/" + identityToDelete;
        var keysStoragePath = RadStub.KEYS_STORAGE_PATH + "/" + identityToDelete;
        var params = cmd.getParametersList();
        if (SystemInfo.isWindows) {
            assertThat(params.get(2)).contains("rm -rf " + gitStoragePath + " " + keysStoragePath);
        } else {
            assertThat(cmd.getExePath()).isEqualTo("rm");
            assertThat(params.get(0)).isEqualTo("-rf");
            assertThat(params.get(1)).isEqualTo(gitStoragePath);
            assertThat(params.get(2)).isEqualTo(keysStoragePath);
        }
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radNotification_AuthRemoveIdentity"));
    }

    @Test
    public void createIdentityTest() throws InterruptedException {

        var identitiesView = new RadicleSettingsIdentitiesView();
        RadicleSettingsIdentitiesView.AddProfileButton createNewIdentity = identitiesView.new AddProfileButton();
        createNewIdentity.addProfile("test", "test");

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("auth --init --name test --passphrase test");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("The new identity was created successfully"));
    }

    @Test
    public void radTrackAction() throws InterruptedException {
        final var node = new RadTrack.SeedNode("http://pine.radicle.com");
        var track = new RadTrack(firstRepo, node);
        track.run();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track --seed " + node.url() + " --remote");
    }

    @Test
    public void radTrackPeerAction() throws InterruptedException {
        var trackPeer = new RadTrack(firstRepo, new RadTrack.Peer("123"));
        trackPeer.perform();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track 123");
    }

    @Test
    public void radPullAction() throws InterruptedException {
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadPull(null).getNotificationSuccessMessage());
    }

    @Test
    public void radSyncAction() throws InterruptedException {
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadSync(null).getNotificationSuccessMessage());
    }

    @Test
    public void testSuccessNotificationAfterInstalled() throws InterruptedException {
        radicleSettingsHandler.savePath("");
        var rm = new RadicleManagerListener();
        rm.runActivity(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("installedSuccessfully"));

        radicleSettingsHandler.savePath(RAD_PATH);
        rm.runActivity(getProject());
        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNull();
    }

    @Test
    public void cliConfiguredError() throws InterruptedException {
        radicleSettingsHandler.savePath("");
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        var rps = new RadicleSyncAction();
        rps.performAction(getProject(), List.of(firstRepo));

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radCliPathMissingText"));

        radicleSettingsHandler.savePath(RAD_PATH);
    }

    @Test
    public void testRadInitError() throws InterruptedException {
        removeRemoteRadUrl(firstRepo);
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        initializeProject(firstRepo);
    }
}
