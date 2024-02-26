package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.EDT;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.IdentityDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.auth.AuthService;
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
        this(repo.getProject(), repo);
    }

    public RadAction(Project project, GitRepository repo) {
        this.project = project;
        this.repo = repo;
    }

    public abstract ProcessOutput run();

    public abstract String getActionName();

    public boolean shouldShowNotification() {
        return true;
    }

    public boolean shouldUnlockIdentity() {
        return false;
    }

    public ProcessOutput perform(String radHome, String radPath, IdentityDialog dialog) {
        return perform(new CountDownLatch(1), radHome, radPath, dialog);
    }

    public ProcessOutput perform(IdentityDialog dialog) {
        var projectSettings = getProjectSettings();
        return perform(new CountDownLatch(1), projectSettings.getRadHome(), projectSettings.getPath(), dialog);
    }

    public ProcessOutput perform() {
        var projectSettings = getProjectSettings();
        return perform(new CountDownLatch(1), projectSettings.getRadHome(), projectSettings.getPath(), null);
    }

    public ProcessOutput perform(CountDownLatch latch) {
        var projectSettings = getProjectSettings();
        return perform(latch, projectSettings.getRadHome(), projectSettings.getPath(), null);
    }

    private RadicleProjectSettings getProjectSettings() {
        var pr = project != null ? project : repo.getProject();
        var projectHandler = new RadicleProjectSettingsHandler(pr);
        return projectHandler.loadSettings();
    }

    private ProcessOutput unlockIdentity(String radHome, String radPath, IdentityDialog dialog) {
        if (project == null && repo == null) {
            return new ProcessOutput(0);
        }
        var pr = project != null ? project : repo.getProject();
        var rad = pr.getService(RadicleProjectService.class);
        var output = rad.self(radHome, radPath);
        var lines = output.getStdoutLines(true);
        var radDetails = new RadDetails(lines);
        var isIdentityUnlocked = rad.isIdentityUnlocked(radDetails.keyHash);
        var hasIdentity = RadAction.isSuccess(output);
        var projectSettings = new RadicleProjectSettingsHandler(pr);
        String storedPassword =  null;
        if (hasIdentity) {
            storedPassword = projectSettings.getPassword(radDetails.nodeId);
        }
        var hasStoredPassword = storedPassword != null;
        var showDialog = ((!hasIdentity || !isIdentityUnlocked) && !hasStoredPassword);
        IdentityDialog.IdentityDialogData dialogData = null;
        if (showDialog) {
            var authService = project.getService(AuthService.class);
            var title = !hasIdentity ? RadicleBundle.message("newIdentity") :
                    RadicleBundle.message("unlockIdentity");
            dialogData = authService.showIdentityDialog(title, hasIdentity, dialog);
        }
        if (!isIdentityUnlocked && hasIdentity && hasStoredPassword) {
            var authOutput = rad.auth(storedPassword, "", radHome, radPath);
            var success = RadAuth.validateOutput(authOutput);
            if (!RadAction.isSuccess(success)) {
                projectSettings.savePassphrase(radDetails.nodeId, null);
            }
            return success;
        }
        if (dialogData != null) {
              var authOutput = rad.auth(dialogData.passphrase(), dialogData.alias(), radHome, radPath);
              var success = RadAuth.validateOutput(authOutput);
              if (RadAction.isSuccess(success)) {
                  output = rad.self(radHome, radPath);
                  lines = output.getStdoutLines(true);
                  radDetails = new RadDetails(lines);
                  projectSettings.savePassphrase(radDetails.nodeId, dialogData.passphrase());
              }
              return success;
        }
        var newOutput = new ProcessOutput(showDialog ? -1 : 0);
        newOutput.appendStderr(RadicleBundle.message("unableToUnlock"));
        return newOutput;
    }

    public ProcessOutput perform(CountDownLatch latch, String radHome, String radPath, IdentityDialog dialog) {
        ProcessOutput output = null;
        var unlockIdentity = shouldUnlockIdentity();
        if (unlockIdentity) {
            output = unlockIdentity(radHome, radPath, dialog);
        }
        if ((output != null && RadAction.isSuccess(output)) || !unlockIdentity) {
            output = this.run();
        }
        var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleProjectService.class));
        latch.countDown();
        if (!success) {
            logger.warn(this.getActionName() + ": " + this.getErrorMessage() + ": exit:{}, out:{} err:{}",
                    output.getExitCode(), output.getStdout(), output.getStderr());
            if (shouldShowNotification()) {
                var errorMsg = output.getStderr();
                var outputMsg = output.getStdout();
                showErrorNotification(project, "radCliError", !Strings.isNullOrEmpty(errorMsg) ? errorMsg : outputMsg);
            }
            return output;
        }
        logger.warn(this.getActionName() + ": " + this.getSuccessMessage() + ": exit:{}, out:{} err:{}",
                output.getExitCode(), output.getStdout(), output.getStderr());
        if (!this.getNotificationSuccessMessage().isEmpty() && shouldShowNotification()) {
            showNotification(project, "", this.getNotificationSuccessMessage(), NotificationType.INFORMATION, this.notificationActions());
        }
        return output;
    }

    public String getErrorMessage() {
        return RadicleBundle.messageOrEmpty("radError_" + getActionName());
    }

    public String getSuccessMessage() {
        return RadicleBundle.messageOrEmpty("radSuccess_" + getActionName());
    }

    public String getNotificationSuccessMessage() {
        return RadicleBundle.messageOrEmpty("radNotification_" + getActionName());
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
                if (isProjectInitialized) {
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
        if (repo == null) {
            return false;
        }
        var remotes = repo.getRemotes();
        for (var remote : remotes) {
            for (var url : remote.getUrls()) {
                if (url.startsWith("rad://")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<GitRepository> getNonConfiguredRepos(List<GitRepository> repos) {
        var nonConfiguredRepos = new ArrayList<GitRepository>();
        for (var repo : repos) {
            try {
                var isConfigured = isProjectRadInitialized(repo);
                if (!isConfigured) {
                    nonConfiguredRepos.add(repo);
                }
            } catch (Exception e) {
                logger.warn("unable to read git config file", e);
            }
        }
        return nonConfiguredRepos;
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
        var rsh = new RadicleProjectSettingsHandler(project);
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

    public static void showErrorNotification(Project project, String title, String content) {
        showNotification(project, title, content, NotificationType.ERROR, null);
    }

    public static void showNotification(
            Project project, String title, String content, NotificationType type,
            List<NotificationAction> actions) {
        final var nfType = type != null ? type : NotificationType.ERROR;
        if (!EDT.isCurrentThreadEdt()) {
            ApplicationManager.getApplication().invokeLater(() -> showNotification(project, title, content, nfType, actions));
            return;
        }
        var nf = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP).createNotification(RadicleBundle.message(content), nfType);
        nf.setTitle(Strings.isNullOrEmpty(title) ? "" : RadicleBundle.message(title));
        if (actions != null && !actions.isEmpty()) {
            for (var action : actions) {
                nf.addAction(action);
            }
        }
        nf.notify(project);
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
