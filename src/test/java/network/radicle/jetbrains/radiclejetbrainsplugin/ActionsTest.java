package network.radicle.jetbrains.radiclejetbrainsplugin;

import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
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
        var clone = new RadClone(radUrl, "C:\\", getProject());
        clone.perform();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("clone " + radUrl);
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
    public void radInspectAction() throws InterruptedException {
        var inspectAction = new RadInspect(firstRepo);
        inspectAction.perform();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("inspect");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNull();
    }

    @Test
    public void radFetchAction() throws InterruptedException {
        var rfa = new RadicleSyncAction();
        rfa.performAction(getProject());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync -f");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadSync(null).getNotificationSuccessMessage());
    }

    @Test
    public void testSuccessNotificationAfterInstalled() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("");
        var rm = new RadicleManagerListener();
        rm.runActivity(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("installedSuccessfully"));

        radicleProjectSettingsHandler.savePath(RAD_PATH);
        rm.runActivity(getProject());
        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNull();
    }

    @Test
    public void cliConfiguredError() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("");
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        var rps = new RadicleSyncAction();
        rps.performAction(getProject(), List.of(firstRepo));

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radCliPathMissingText"));

        radicleProjectSettingsHandler.savePath(RAD_PATH);
    }

    @Test
    public void testRadInitError() throws InterruptedException {
        removeRemoteRadUrl(firstRepo);
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        initializeProject(firstRepo);
    }
}
