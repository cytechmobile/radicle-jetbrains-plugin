package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class BasicAction {
    private static final Logger logger = LoggerFactory.getLogger(BasicAction.class);
    public static final String NOTIFICATION_GROUP = "Radicle.NotificationGroup";

    private RadAction action;
    private Project project;
    private CountDownLatch countDownLatch;


    public BasicAction(@NotNull RadAction action, Project project, @NotNull CountDownLatch countDownLatch) {
        this.action = action;
        this.project = project;
        this.countDownLatch = countDownLatch;
    }

    public ProcessOutput perform() {
        var output = action.run();
        var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleApplicationService.class));
        countDownLatch.countDown();
        if (!success) {
            logger.warn(action.getErrorMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
            showErrorNotification(project, "radCliError", output.getStderr());
            return output;
        }
        logger.info(action.getSuccessMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
        if (!action.getNotificationSuccessMessage().isEmpty()) {
            showNotification(project, "", action.getNotificationSuccessMessage(), NotificationType.INFORMATION, null);
        }
        return output;
    }

    public static List<GitRepository> getInitializedReposWithNodeConfigured(List<GitRepository> repos, boolean showNotification) {
        var initializedRepos = new java.util.ArrayList<>(List.<GitRepository>of());
        for (var repo : repos) {
            try {
                var remote = GitConfigUtil.getValue(repo.getProject(), repo.getRoot(), "remote.rad.url");
                if (!Strings.isNullOrEmpty(remote) && isSeedNodeConfigured(repo)) {
                    initializedRepos.add(repo);
                }
            } catch (Exception e) {
                logger.warn("unable to read git config file", e);
            }
        }
        if (showNotification && initializedRepos.isEmpty()) {
            showErrorNotification(null, "radCliError", RadicleBundle.message("initializationError"));
        }
        return initializedRepos;
    }

    private static boolean isSeedNodeConfigured(GitRepository repo) {
        var seedNodes = List.of("https://pine.radicle.garden", "https://willow.radicle.garden", "https://maple.radicle.garden");
        boolean hasSeedNode = false;
        try {
            var seed = GitConfigUtil.getValue(repo.getProject(), repo.getRoot(), "rad.seed");
            if (!Strings.isNullOrEmpty(seed)) {
                for (String node : seedNodes) {
                    hasSeedNode = seed.contains(node);
                    if (hasSeedNode) break;
                }
            }
        } catch (Exception e) {
            logger.warn("unable to read git config file", e);
        }
        if (!hasSeedNode) {
            showErrorNotification(repo.getProject(), "radCliError", RadicleBundle.message("seedNodeMissing"));
        }
        return hasSeedNode;
    }

    public static boolean isCliPathConfigured(Project project) {
        var rsh = new RadicleSettingsHandler();
        var rs = rsh.loadSettings();
        logger.debug("settings are: {}", rs);
        // check if rad cli is configured
        if (Strings.isNullOrEmpty(rs.getPath())) {
            logger.warn("no rad cli path configured");
            showNotification(project, "radCliPathMissing", "radCliPathMissingText", NotificationType.WARNING,
                    List.of(new ConfigureRadCliNotificationAction(null, RadicleBundle.lazyMessage("configure"))));
            return false;
        }
        return true;
    }

    public static void showErrorNotification(Project project, String title, String content) {
        showNotification(project, title, content, NotificationType.ERROR, null);
    }

    public static void showNotification(
            Project project, String title, String content, NotificationType type,
            List<NotificationAction> actions) {
        type = type != null ? type : NotificationType.ERROR;
        var notif = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(RadicleBundle.message(content),type);
        notif.setTitle(Strings.isNullOrEmpty(title) ? "" : RadicleBundle.message(title));
        if (actions != null && !actions.isEmpty()) {
            for (var action : actions) {
                notif.addAction(action);
            }
        }
        notif.notify(project);
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
