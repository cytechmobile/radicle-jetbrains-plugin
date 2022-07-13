package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.google.common.base.Strings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RadiclePushAction extends AnAction {
    private static final Logger logger = LoggerFactory.getLogger(RadiclePushAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        logger.debug("action performed: {}", e);
        var rsh = new RadicleSettingsHandler();
        var rs = rsh.loadSettings();
        logger.debug("settings are: {}", rs);

        // check if rad cli is configured
        if (Strings.isNullOrEmpty(rs.getPath())) {
            logger.warn("no rad cli path configured");
            final var project = e.getProject();
            var rb = new RadicleBundle();
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("Radicle.NotificationGroup")
                    .createNotification(
                            rb.getMessage("radCliPathMissing"),
                            rb.getMessage("radCliPathMissingText"),
                            NotificationType.WARNING)
                    .addAction(new ConfigureRadCliNotificationAction(project, rb.getLazyMessage("configure")))
                    .notify(e.getProject());
        }

        // TODO: could be useful: PersistentDefaultAccountHolder and GithubProjectDefaultAccountHolder
    }

    public static class ConfigureRadCliNotificationAction extends NotificationAction {
        final Project project;

        public ConfigureRadCliNotificationAction(Project p, Supplier<String> msg) {
            super(msg);
            this.project = p;
        }
        @Override
        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
            logger.debug("clicked configure rad cli notification action");
            notification.hideBalloon();
            ShowSettingsUtil.getInstance().showSettingsDialog(project, RadicleSettingsView.class);
        }
    }
}
