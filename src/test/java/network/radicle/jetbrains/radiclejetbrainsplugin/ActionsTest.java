package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.dvcs.push.PushInfo;
import com.intellij.dvcs.push.PushSource;
import com.intellij.dvcs.push.PushSpec;
import com.intellij.dvcs.push.PushTarget;
import com.intellij.dvcs.repo.Repository;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.vcs.log.VcsFullCommitDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePullAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePushAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.listeners.RadicleManagerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ActionsTest extends AbstractIT {

    @Test
    public void testRadPushAction() throws InterruptedException {
        List<PushInfo> details = new ArrayList<>();
        details.add(new PushDetails());
        var srv = super.getProject().getService(RadicleProjectService.class);
        srv.pushDetails = details;
        srv.forceRadPush = true;

        var notif = NotificationGroupManager.getInstance()
                .getNotificationGroup("Vcs Notifications")
                .createNotification(
                        RadicleBundle.message("test") ,"Pushed 1 commit", NotificationType.INFORMATION,null);

        notif.notify(super.getProject());

        assertPushAction();

        /* enable auto sync */
        radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.YES);
        notif.notify(super.getProject());
        assertPushAction();
    }

    @Test
    public void radPullAction() throws InterruptedException {
        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());
        var result = rpa.getUpdateCountDown().await(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertThat(cmd).isNotNull();
        assertCmd(cmd);

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadPull().getNotificationSuccessMessage());
    }

    @Test
    public void radSyncAction() throws InterruptedException {
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var result = rsa.getUpdateCountDown().await(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");
        var not = notificationsQueue.poll(10,TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(new RadSync().getNotificationSuccessMessage());
    }

    @Test
    public void testSuccessNotificationAfterInstalled() throws InterruptedException {
        radicleSettingsHandler.savePath("");
        var rm = new RadicleManagerListener();
        rm.projectOpened(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getContent()).contains(RadicleBundle.message("installedSuccessfully"));

        radicleSettingsHandler.savePath(radPath);
        rm.projectOpened(getProject());
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

        var rps = new RadiclePushAction();
        var actionEvent = AnActionEvent.createFromAnAction(rps, null, "somewhere", dataId -> "test");
        rps.performAction(getProject(),actionEvent);

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("radCliPathMissingText"));

        radicleSettingsHandler.savePath(radPath);
    }

    @Test
    public void testSeedNodeError() throws InterruptedException {
        removeSeedNodeFromConfig();

        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("seedNodeMissing"));

        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("seedNodeMissing"));

        var rps = new RadiclePushAction();
        var actionEvent = AnActionEvent.createFromAnAction(rps, null, "somewhere", dataId -> "test");
        rps.performAction(getProject(),actionEvent);

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("seedNodeMissing"));

        addSeedNodeInConfig();
    }

    @Test
    public void testRadInitError() throws InterruptedException {
        removeRemoteRadUrl();
        var rsa = new RadicleSyncAction();
        rsa.performAction(getProject());

        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        var rpa = new RadiclePullAction();
        rpa.performAction(getProject());

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        var rps = new RadiclePushAction();
        var actionEvent = AnActionEvent.createFromAnAction(rps, null, "somewhere", dataId -> "test");
        rps.performAction(getProject(),actionEvent);

        not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not.getContent()).isEqualTo(RadicleBundle.message("initializationError"));

        initializeProject();
    }

    public  void assertPushAction() throws InterruptedException {
        var srv = super.getProject().getService(RadicleProjectService.class);
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("sync");
        /* catch custom notification */
        var not1 = notificationsQueue.poll(10, TimeUnit.SECONDS);
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);

        assertThat(not).isNotNull();
        assertThat(not1).isNotNull();
        boolean hasSuccessSyncNotification = false;

        if (not1.getContent().contains(new RadSync().getNotificationSuccessMessage()) ||
                not.getContent().contains(new RadSync().getNotificationSuccessMessage())) {
            hasSuccessSyncNotification = true;
        }
        assertThat(hasSuccessSyncNotification).isTrue();
        assertThat(srv.forceRadPush).isFalse();
    }



    private class PushDetails implements PushInfo {

        @Override
        public @NotNull Repository getRepository() {
            return repository;
        }

        @Override
        public @NotNull PushSpec<PushSource, PushTarget> getPushSpec() {
            return null;
        }

        @Override
        public @NotNull List<VcsFullCommitDetails> getCommits() {
            return null;
        }
    }
}
