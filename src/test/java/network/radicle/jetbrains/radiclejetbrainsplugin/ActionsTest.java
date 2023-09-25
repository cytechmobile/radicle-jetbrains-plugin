package network.radicle.jetbrains.radiclejetbrainsplugin;

import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncFetchAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadClone;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSelf;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.listeners.RadicleManagerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void radInspectAction() throws InterruptedException {
        var inspectAction = new RadInspect(firstRepo);
        inspectAction.perform();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("inspect");
        var not = notificationsQueue.poll(100, TimeUnit.MILLISECONDS);
        assertThat(not).isNull();
    }

    @Test
    public void radSelfAction() throws InterruptedException {
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        var radSelf = new RadSelf(getProject());
        radSelf.askForIdentity(false);
        var output = radSelf.perform();

        var aliasCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(aliasCmd);
        assertThat(aliasCmd.getCommandLineString()).contains(RAD_PATH + " " + "self --alias");

        var nidCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(nidCmd);
        assertThat(nidCmd.getCommandLineString()).contains(RAD_PATH + " " + "self --nid");

        var didCmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(didCmd);
        assertThat(didCmd.getCommandLineString()).contains(RAD_PATH + " " + "self --did");

        var fingerPrint = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(fingerPrint);
        assertThat(fingerPrint.getCommandLineString()).contains(RAD_PATH + " " + "self --ssh-fingerprint");

        //Test parsing
        var details = new RadDetails(output.getStdoutLines(true));
        assertThat(details.alias).isEqualTo(RadStub.alias);
        assertThat(details.did).isEqualTo(RadStub.did);
        assertThat(details.nodeId).isEqualTo(RadStub.nodeId);
        assertThat(details.keyHash).isEqualTo(RadStub.keyHash);
    }

    @Test
    public void radSyncAction() throws InterruptedException {
        var rfa = new RadicleSyncAction();
        rfa.performAction(getProject());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");
        assertThat(cmd.getCommandLineString()).doesNotContain("-f");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        var repo = mock(GitRepository.class);
        when(repo.getProject()).thenReturn(getProject());
        assertThat(not.getContent()).contains(new RadSync(repo, false).getNotificationSuccessMessage());
    }

    @Test
    public void radFetchAction() throws InterruptedException {
        var rfa = new RadicleSyncFetchAction();
        rfa.performAction(getProject());
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync -f");
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        var repo = mock(GitRepository.class);
        when(repo.getProject()).thenReturn(getProject());
        assertThat(not.getContent()).contains(new RadSync(repo, true).getNotificationSuccessMessage());
    }

    @Test
    public void testSuccessNotificationAfterInstalled() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("");
        var rm = new RadicleManagerListener();
        rm.execute(getProject(), NoopContinuation.NOOP);

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("installedSuccessfully"));

        radicleProjectSettingsHandler.savePath(RAD_PATH);
        rm.execute(getProject(), NoopContinuation.NOOP);
        not = notificationsQueue.poll(100, TimeUnit.MILLISECONDS);
        assertThat(not).isNull();
    }

    @Test
    public void cliConfiguredError() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("");
        var rsa = new RadicleSyncFetchAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("radCliPathMissingText"));

        var rps = new RadicleSyncFetchAction();
        rps.performAction(getProject(), List.of(firstRepo));

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radCliPathMissingText"));

        var radSyncAction = new RadicleSyncAction();
        radSyncAction.performAction(getProject(), List.of(firstRepo));

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radCliPathMissingText"));

        radicleProjectSettingsHandler.savePath(RAD_PATH);
    }

    @Test
    public void testRadInitError() throws InterruptedException {
        removeRemoteRadUrl(firstRepo);
        var rsa = new RadicleSyncFetchAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        var syncAction = new RadicleSyncFetchAction();
        syncAction.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        initializeProject(firstRepo);
    }
}
