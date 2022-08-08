package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadiclePushAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.SelectActionDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RadicleProjectService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RadicleProjectService.class);
    private static final String GIT_PUSH_DISPLAY_ID = "git.push.result";

    private Project project = null;
    public List<PushInfo> pushDetails;
    public boolean forceRadPush;

    public RadicleProjectService(Project project) {
        logger.info(RadicleBundle.message("projectService", project.getName()));
        this.project = project;
        setupNotificationListener();
    }

    protected void setupNotificationListener() {
        logger.warn("setting up notification listener");
        project.getMessageBus().connect(project).subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(@NotNull Notification notification) {
                /* Check intellij notification for successful git push message */
                if (!isGitPushNotification(notification)) {
                    logger.debug("not handling non-git-push notification: {}", notification);
                    return;
                }
                var rsh = new RadicleSettingsHandler();
                var rs = rsh.loadSettings();
                /* Check if the user has configured rad */
                if (Strings.isNullOrEmpty(rs.getPath())) {
                    logger.debug("no path configured, not handling git-push notification: {} settings:{}",
                            notification, rs);
                    return;
                }

                if (pushDetails == null || pushDetails.isEmpty()) {
                    logger.debug("no push details found, not handling git-push notification : {} settings:{}",
                            notification, rs);
                    return;
                }

                var repos = pushDetails.stream().map(detail -> (GitRepository) detail.getRepository())
                        .collect(Collectors.toList());
                var radSync = rs.getRadSync();
                if (forceRadPush || radSync == RadicleSettings.RadSyncType.YES.val) {
                    forceRadPush = false;
                    var syncAction = new RadicleSyncAction();
                    syncAction.performAction(project, repos);
                    /* Check if user has configured plugin to run automatically sync action */
                } else if(radSync == RadicleSettings.RadSyncType.ASK.val) {
                    var dialog = new SelectActionDialog(project, repos);
                    dialog.showAndGet();
                }
            }
        });
    }

    private boolean isGitPushNotification(Notification notification) {
        return notification.getDisplayId() != null && notification.getDisplayId().equals(GIT_PUSH_DISPLAY_ID) &&
                notification.getType().equals(NotificationType.INFORMATION);
    }

}