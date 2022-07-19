package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.SelectActionDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RadicleProjectService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RadicleProjectService.class);

    private Project project = null;
    private final String GIT_PUSH_DISPLAY_ID = "git.push.result";
    public List<PushInfo> pushDetails;
    public boolean forceRadSync;

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
                if (isGitPushNotification(notification)) {
                    var rsh = new RadicleSettingsHandler();
                    var rs = rsh.loadSettings();
                    /* Check if the user has configured rad */
                    if (!Strings.isNullOrEmpty(rs.getPath())) {
                        /* Check if user has configured plugin to run automatically */
                        var radSync = rs.getRadSync();
                        if (forceRadSync || (Boolean.parseBoolean(radSync))) {
                            forceRadSync = false;
                            ApplicationManager.getApplication().executeOnPooledThread(() ->
                                    RadicleSyncAction.sync(pushDetails, project));
                        } else if (Strings.isNullOrEmpty(radSync)) {
                            var dialog = new SelectActionDialog(project, pushDetails);
                            dialog.showAndGet();
                        }
                    }
                }
            }
        });
    }

    private boolean isGitPushNotification(Notification notification) {
        return notification.getDisplayId() != null && notification.getDisplayId().equals(GIT_PUSH_DISPLAY_ID) &&
                notification.getType().equals(NotificationType.INFORMATION);
    }

}