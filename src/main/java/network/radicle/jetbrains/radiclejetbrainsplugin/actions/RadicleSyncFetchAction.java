package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class RadicleSyncFetchAction extends AnAction {
    protected CountDownLatch updateCountDown;

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
        final var project = e.getProject();
        assert project != null;
        performAction(project);
    }

    public void performAction(@NotNull Project project) {
        final var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        performAction(project, repos);
    }

    public void performAction(@NotNull Project project, @NotNull List<GitRepository> repos) {
        if (!RadAction.isCliPathConfigured(project)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, true);
            if (radInitializedRepos.isEmpty()) {
                return;
            }
            updateCountDown = new CountDownLatch(radInitializedRepos.size());
            UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message("radFetchProgressTitle"), updateCountDown);
            // run in new thread, otherwise it would be called synchronously on current thread and would block forever
            // since we haven't issued yet the commands that will count down the latch
            new Thread(ubt::queue).start();

            radInitializedRepos.forEach(repo -> {
                var fetch = new RadSync(repo, true);
                fetch.perform(updateCountDown);
            });
        });
    }
}
