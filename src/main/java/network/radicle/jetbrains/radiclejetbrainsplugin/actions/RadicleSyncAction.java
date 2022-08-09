package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadSync;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadicleSyncAction extends AnAction {

    protected CountDownLatch updateCountDown;
    protected AtomicBoolean executingFlag = new AtomicBoolean(false);

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
        this.updateCountDown = new CountDownLatch(repos.size());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (BasicAction.isValidConfiguration(project)) {
                var sync = new RadSync();
                repos.forEach(repo -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                        new BasicAction(sync,repo,project,updateCountDown).perform()));

                UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message(sync.getProgressBarTitle()),
                        updateCountDown,executingFlag);
                new Thread(ubt::queue).start();
            }
        });
    }

    public CountDownLatch getUpdateCountDown() {
        return updateCountDown;
    }
}
