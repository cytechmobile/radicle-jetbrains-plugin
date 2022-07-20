package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.google.common.base.Strings;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.push.PushInfo;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class RadicleSyncAction extends AnAction {
    private static final Logger logger = LoggerFactory.getLogger(RadicleSyncAction.class);

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
            showNotification(e.getProject(), "radCliPathMissing", "radCliPathMissingText", NotificationType.WARNING,
                    List.of(new ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
            return;
        }
        var project = e.getProject();
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        if (repos.isEmpty()) {
            logger.warn("no git repos found!");
            return;
        }
        openGitPushDialog(project, e);
        var rps = project.getService(RadicleProjectService.class);
        rps.forceRadSync = true;
    }

    public static void sync(List<PushInfo> pushDetails, Project project) {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        for (var pi : pushDetails) {
            var repo = (GitRepository) pi.getRepository();
            // TODO: most probably there is a better way to understand if this repo is rad enabled
            var output = rad.sync(repo);
            var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleApplicationService.class));
            if (!success) {
                logger.warn("error in rad inspect: exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
                showErrorNotification(repo.getProject(), "radCliError", output.getStderr());
                return;
            }
            logger.info("success in rad inspect: exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
            showNotification(project, "", "Synced project with radicle seed node", NotificationType.INFORMATION, null);
        }
        // TODO: could be useful: PersistentDefaultAccountHolder and GithubProjectDefaultAccountHolder
    }

    private void openGitPushDialog(Project project , @NotNull AnActionEvent e) {
        VcsRepositoryManager manager = VcsRepositoryManager.getInstance(project);
        Collection<Repository> repositories = e.getData(CommonDataKeys.EDITOR) != null
                ? ContainerUtil.emptyList()
                : collectRepositories(manager, e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));
        VirtualFile selectedFile = DvcsUtil.getSelectedFile(project);
        new VcsPushDialog(project, DvcsUtil.sortRepositories(repositories),
                selectedFile != null ? manager.getRepositoryForFileQuick(selectedFile) : null).show();
    }

    @NotNull
    private static Collection<Repository> collectRepositories(@NotNull VcsRepositoryManager vcsRepositoryManager,
                                                              VirtualFile @Nullable [] files) {
        if (files == null) return Collections.emptyList();
        Collection<Repository> repositories = new HashSet<>();
        for (VirtualFile file : files) {
            Repository repo = vcsRepositoryManager.getRepositoryForFileQuick(file);
            if (repo != null) {
                repositories.add(repo);
            }
        }
        return repositories;
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

    public static void showErrorNotification(Project project, String title, String content) {
        showNotification(project, title, content, NotificationType.ERROR, null);
    }

    public static void showNotification(
            Project project, String title, String content, NotificationType type,
            Collection<NotificationAction> actions) {
        type = type != null ? type : NotificationType.ERROR;
        var notif = NotificationGroupManager.getInstance()
                .getNotificationGroup("Radicle.NotificationGroup")
                .createNotification(
                        Strings.isNullOrEmpty(title) ? "" : RadicleBundle.message(title),
                        RadicleBundle.message(content), type);
        if (actions != null && !actions.isEmpty()) {
            notif.addActions(actions);
        }
        notif.notify(project);
    }
}
