package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.PublishDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RadiclePublishAction extends AnAction {
    private static final Logger logger = LoggerFactory.getLogger(RadiclePublishAction.class);

    private List<GitRepository> nonConfiguredRepos = null;

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (nonConfiguredRepos != null && nonConfiguredRepos.isEmpty()) {
            presentation.setEnabledAndVisible(false);
        }
        var project = e.getProject();
        if (project == null) {
            return;
        }
        final var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            nonConfiguredRepos = RadAction.getNonConfiguredRepos(repos);
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!RadAction.isCliPathConfigured(e.getProject())) {
            return;
        }
        var project = e.getProject();
        var publishDialog = new PublishDialog(nonConfiguredRepos, project);
        try {
            publishDialog.isUiLoaded.await();
            publishDialog.showAndGet();
        } catch (InterruptedException ex) {
            logger.warn("Unable to open publish dialog");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
