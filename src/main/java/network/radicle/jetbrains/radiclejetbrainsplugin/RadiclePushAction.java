package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
            RadicleSyncAction.showNotification(e.getProject(), "radCliPathMissing", "radCliPathMissingText", NotificationType.WARNING,
                    List.of(new RadicleSyncAction.ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
            return;
        }

        var project = e.getProject();
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        if (repos.isEmpty()) {
            logger.warn("no git repos found!");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            for (var repo : repos) {
                var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
                var output = rad.push(repo);
                var success = output.checkSuccess(com.intellij.openapi.diagnostic.Logger.getInstance(RadicleApplicationService.class));
                if (!success) {
                    logger.warn("error in rad push: exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
                    RadicleSyncAction.showErrorNotification(repo.getProject(), "radCliError", output.getStderr());
                    return;
                }
                logger.info("success in rad push: exit:{}, out:{} err:{}", output.getExitCode(), output.getStdout(), output.getStderr());
                RadicleSyncAction.showNotification(project, "", "Project synced", NotificationType.INFORMATION, null);
            }
        });
    }
}
