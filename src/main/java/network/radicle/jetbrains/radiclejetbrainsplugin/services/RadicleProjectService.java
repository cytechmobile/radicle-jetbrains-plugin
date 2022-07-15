package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.base.Strings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.SelectActionDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.listeners.RadiclePrePushListener;
import org.jetbrains.annotations.NotNull;

public class RadicleProjectService {

    private static final Logger logger = Logger.getInstance(RadicleProjectService.class);
    private Project project = null;
    private final String GIT_PUSH_DISPLAY_ID = "git.push.result";

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
                    var pushDetails = RadiclePrePushListener.getPushDetails();
                    /* Check if the user has configured rad */
                    if (!Strings.isNullOrEmpty(rs.getPath())) {
                        /* Check if user has configure plugin to run automatically rad sync */
                        var radSync = rs.getRadSync();
                        if (radSync != null && radSync) {
                            //TODO run rad sync command
                        } else if(radSync == null) {
                            var dialog = new SelectActionDialog(pushDetails);
                            dialog.showAndGet();
                        }
                    }
                }
            }
        });
    }

    private boolean isGitPushNotification(Notification notification) {
        return notification.getDisplayId().equals(GIT_PUSH_DISPLAY_ID) &&
                notification.getType().equals(NotificationType.INFORMATION);
    }

}