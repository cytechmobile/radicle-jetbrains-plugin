package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.PublishDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadiclePublishAction extends AnAction  {

    private List<GitRepository> nonConfiguredRepos = null;

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (nonConfiguredRepos != null) {
            var showShareAction = !nonConfiguredRepos.isEmpty();
            presentation.setEnabledAndVisible(showShareAction);
            return ;
        }
        var project = e.getProject();
        final var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            nonConfiguredRepos = BasicAction.getNonConfiguredRepos(repos);
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!BasicAction.isCliPathConfigured(e.getProject())) {
            return ;
        }
        var project = e.getProject();
        var publishDialog = new PublishDialog(nonConfiguredRepos, project);
        publishDialog.showAndGet();
    }
}
