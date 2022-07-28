package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadSync;
import org.jetbrains.annotations.NotNull;

public class RadicleSyncEvent extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();

        if (!BasicAction.isCliPathConfigured(e) || !BasicAction.hasGitRepos(e)) {
            return ;
        }

        var basicAction = new BasicAction(new RadSync());
        basicAction.perform(repos,project);
    }
}
