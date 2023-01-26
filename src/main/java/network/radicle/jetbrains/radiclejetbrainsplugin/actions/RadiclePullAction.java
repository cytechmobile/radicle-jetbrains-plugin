package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

public class RadiclePullAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        RadAction.showRadIcon(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        performAction(project);
    }

    public void performAction(Project project) {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        if (!RadAction.isCliPathConfigured(project)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, true);
            if (radInitializedRepos.isEmpty()) {
                return;
            }
            final var updateCountDown = new CountDownLatch(radInitializedRepos.size());
            radInitializedRepos.forEach(repo -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var pull = new RadPull(repo);
                pull.perform(updateCountDown);
            }));
            var ubt = new UpdateBackgroundTask(project, RadicleBundle.message("radPullProgressTitle"), updateCountDown);
            new Thread(ubt::queue).start();
        });
    }

}
