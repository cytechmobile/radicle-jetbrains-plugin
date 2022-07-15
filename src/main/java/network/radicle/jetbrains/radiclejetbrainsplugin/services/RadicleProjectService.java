package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.dvcs.push.PushSupport;
import com.intellij.dvcs.push.Pusher;
import com.intellij.dvcs.push.VcsPushOptionValue;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RadicleProjectService {

    private static final Logger logger = Logger.getInstance(RadicleProjectService.class);
    private Project project = null;
    private static String GIT_PUSH_DISPLAY_ID = "git.push.result";

    public RadicleProjectService(Project project) {
        logger.info(RadicleBundle.message("projectService", project.getName()));
        this.project = project;
        setupGitPushListener();
        setupPushSupport();
    }

    protected void setupPushSupport() {
        logger.warn("setting up push support");
        try {
            var pushSupports = PushSupport.PUSH_SUPPORT_EP.getExtensions(project);
            PushSupport gps = null;
            for (var ps : pushSupports) {
                if (ps.getVcs().getName().equalsIgnoreCase("Git")) {
                    gps = ps;
                    break;
                }
            }
            if (gps == null) {
                logger.warn("no git push support found!");
                return;
            }
            final var originalGitPusher = gps.getPusher();
            var fields = gps.getClass().getDeclaredFields();
            for (var field : fields) {
                if (field.getName().equals("myPusher")) {
                    field.setAccessible(true);
                    field.set(gps, new Pusher() {
                        @Override
                        public void push(@NotNull Map pushSpecs, @Nullable VcsPushOptionValue additionalOption, boolean force) {
                            originalGitPusher.push(pushSpecs, additionalOption, force);
                            logger.warn("from override pusher !!!!");
                        }
                    });
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
    }

    protected void setupGitPushListener() {
        logger.warn("setting up notification listener");
        project.getMessageBus().connect(project).subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(@NotNull Notification notification) {
                /* Check intellij notification for successful git push message */
                if (notification.getDisplayId().equals(GIT_PUSH_DISPLAY_ID) &&
                        notification.getType().equals(NotificationType.INFORMATION)) {

                }
            }
        });
    }
}