package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public abstract class RadAction {
    private static final Logger logger = LoggerFactory.getLogger(RadAction.class);
    public static final String NOTIFICATION_GROUP = "Radicle.NotificationGroup";

    protected Project project;
    protected GitRepository repo;

    public RadAction() {
        this(null, null);
    }

    public RadAction(Project project) {
        this(project, null);
    }

    public RadAction(GitRepository repo) {
        this(null, repo);
    }

    public RadAction(Project project, GitRepository repo) {
        this.project = project;
        this.repo = repo;
    }

    public abstract ProcessOutput run();

    public abstract String getActionName();

    public boolean showNotification() {
        return true;
    }

    public ProcessOutput perform() {
        return perform(new CountDownLatch(1));
    }

    public ProcessOutput perform(CountDownLatch latch) {
        var output = this.run();
        var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleApplicationService.class));
        latch.countDown();
        if (!success) {
            logger.warn(this.getErrorMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
            if (showNotification()) {
                showErrorNotification(project, "radCliError", output.getStderr());
            }
            return output;
        }
        logger.info(this.getSuccessMessage() + ": exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
        if (!this.getNotificationSuccessMessage().isEmpty() && showNotification()) {
            showNotification(project, "", this.getNotificationSuccessMessage(),
                    NotificationType.INFORMATION, this.notificationActions());
        }
        return output;
    }

    public String getErrorMessage() {
        return RadicleBundle.message("radError_" + getActionName(), "");
    }

    public String getSuccessMessage() {
        return RadicleBundle.message("radSuccess_" + getActionName(), "");
    }

    public String getNotificationSuccessMessage() {
        return RadicleBundle.message("radNotification_" + getActionName(), "");
    }

    public GitRepository getRepo() {
        return repo;
    }

    public List<NotificationAction> notificationActions() {
        return null;
    }

    public static List<GitRepository> getInitializedReposWithNodeConfigured(List<GitRepository> repos, boolean showNotification) {
        var initializedRepos = new ArrayList<GitRepository>();
        for (var repo : repos) {
            try {
                var isProjectInitialized = isProjectRadInitialized(repo);
                if (isProjectInitialized && isSeedNodeConfigured(repo, true)) {
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

    public static boolean isProjectRadInitialized(GitRepository repo) {
        try {
            var handler = new GitLineHandler(repo.getProject(), repo.getRoot(), GitCommand.CONFIG);
            handler.setSilent(true);
            handler.addParameters("--null", "--local", "--get", "remote.rad.url");
            var remote = Git.getInstance().runCommand(handler).getOutputOrThrow(1).trim();
            return !Strings.isNullOrEmpty(remote);
        } catch (Exception e) {
            logger.warn("unable to read git config file", e);
        }
        return false;
    }

    public static List<GitRepository> getNonConfiguredRepos(List<GitRepository> repos) {
        var nonConfiguredRepos = new ArrayList<GitRepository>();
        for (var repo : repos) {
            try {
                var isConfigured = isSeedNodeConfigured(repo, false);
                if (!isConfigured) {
                    nonConfiguredRepos.add(repo);
                }
            } catch (Exception e) {
                logger.warn("unable to read git config file", e);
            }
        }
        return nonConfiguredRepos;
    }

    private static boolean isSeedNodeConfigured(GitRepository repo, boolean showNotification) {
        var seedNodes = List.of("https://pine.radicle.garden", "https://willow.radicle.garden", "https://maple.radicle.garden");
        boolean hasSeedNode = false;
        try {
            var handler = new GitLineHandler(repo.getProject(), repo.getRoot(), GitCommand.CONFIG);
            handler.setSilent(true);
            handler.addParameters("--null", "--local", "--get", "rad.seed");
            var seed = Git.getInstance().runCommand(handler).getOutputOrThrow(1).trim();
            if (!Strings.isNullOrEmpty(seed)) {
                for (String node : seedNodes) {
                    hasSeedNode = seed.contains(node);
                    if (hasSeedNode) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("unable to read git config file", e);
        }
        if (!hasSeedNode && showNotification) {
            showErrorNotification(repo.getProject(), "radCliError", RadicleBundle.message("seedNodeMissing"));
        }
        return hasSeedNode;
    }

    public static void showRadIcon(@NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        final var gitRepoManager = GitRepositoryManager.getInstance(e.getProject());
        var repos = gitRepoManager.getRepositories();
        e.getPresentation().setEnabledAndVisible(repos.size() > 0);
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
        var notif = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP).createNotification(RadicleBundle.message(content), type);
        notif.setTitle(Strings.isNullOrEmpty(title) ? "" : RadicleBundle.message(title));
        if (actions != null && !actions.isEmpty()) {
            for (var action : actions) {
                notif.addAction(action);
            }
        }
        notif.notify(project);
    }

    public static boolean isSuccess(ProcessOutput out) {
        return !out.isTimeout() && !out.isCancelled() && out.getExitCode() == 0;
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
