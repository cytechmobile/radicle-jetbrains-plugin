package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.google.common.base.Strings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import git4idea.config.GitConfigUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class BasicAction {
    private static final Logger logger = LoggerFactory.getLogger(BasicAction.class);
    public static final String NOTIFICATION_GROUP = "Radicle.NotificationGroup";

    private RadAction action;
    private GitRepository repo;
    private Project project;
    private final CountDownLatch countDownLatch;

    public BasicAction(@NotNull RadAction action, @NotNull GitRepository repo, @NotNull Project project,
                       @NotNull CountDownLatch countDownLatch) {
        this.action = action;
        this.project = project;
        this.repo = repo;
        this.countDownLatch = countDownLatch;
    }

    public void perform() {
        var output = action.run(repo);
        var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleApplicationService.class));
        countDownLatch.countDown();
        //TODO maybe show notification inside Update Background Class
        if (!success) {
            logger.warn(action.getErrorMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
            showErrorNotification(repo.getProject(), "radCliError", output.getStderr());
            return;
        }
        logger.info(action.getSuccessMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
        showNotification(project, "", action.getNotificationSuccessMessage(), NotificationType.INFORMATION, null);
    }

    public static boolean isValidConfiguration(@NotNull Project project) {
        if (!isCliPathConfigured(project) || !hasGitRepos(project) ||
                !isSeedNodeConfigured(project) || !isRadInitialized(project)) {
            return false;
        }
        return true;
    }

    private static boolean isRadInitialized(@NotNull Project project) {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        try {
            var remote = GitConfigUtil.getValue(project, repos.get(0).getRoot(), "remote.rad.url");
            if (!Strings.isNullOrEmpty(remote)) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("unable to read git config file", e);
        }
        showErrorNotification(project, "radCliError", RadicleBundle.message("initializationError"));
        return false;
    }

    private static boolean isSeedNodeConfigured(@NotNull Project project) {
        var seedNodes = List.of("https://pine.radicle.garden", "https://willow.radicle.garden", "https://maple.radicle.garden");
        boolean hasSeedNode = false;
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        try {
            var seed = GitConfigUtil.getValue(project, repos.get(0).getRoot(), "rad.seed");
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
            showErrorNotification(project, "radCliError", RadicleBundle.message("seedNodeMissing"));
        }
        return hasSeedNode;
    }

    private static boolean isCliPathConfigured(@NotNull Project project) {
        var rsh = new RadicleSettingsHandler();
        var rs = rsh.loadSettings();
        logger.debug("settings are: {}", rs);
        // check if rad cli is configured
        if (Strings.isNullOrEmpty(rs.getPath())) {
            logger.warn("no rad cli path configured");
            showNotification(project, "radCliPathMissing", "radCliPathMissingText", NotificationType.WARNING,
                    List.of(new ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
            return false;
        }
        return true;
    }

    private static boolean hasGitRepos(@NotNull Project project) {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        if (repos.isEmpty()) {
            showErrorNotification(project, "radCliError", RadicleBundle.message("noGitRepos"));
            logger.warn("no git repos found!");
            return false;
        }
        return true;
    }

    public static void showErrorNotification(Project project, String title, String content) {
        showNotification(project, title, content, NotificationType.ERROR, null);
    }

    public static void showNotification(
            Project project, String title, String content, NotificationType type,
            Collection<NotificationAction> actions) {
        type = type != null ? type : NotificationType.ERROR;
        var notif = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(
                        Strings.isNullOrEmpty(title) ? "" : RadicleBundle.message(title),
                        RadicleBundle.message(content), type);
        if (actions != null && !actions.isEmpty()) {
            notif.addActions(actions);
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
