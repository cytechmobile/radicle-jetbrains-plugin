package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadPull;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadiclePullAction extends AnAction {

    protected CountDownLatch updateCountDown;
    protected AtomicBoolean executingFlag = new AtomicBoolean(false);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        performAction(project);
    }

    public void performAction(Project project) {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        if (!BasicAction.isCliPathConfigured(project)) {
            return ;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radInitializedRepos = BasicAction.getInitializedReposWithNodeConfigured(repos, true);
            if (radInitializedRepos.isEmpty()) {
                return ;
            }
            updateCountDown = new CountDownLatch(radInitializedRepos.size());
            radInitializedRepos.forEach(repo -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var pull = new RadPull(repo);
                new BasicAction(pull, repo.getProject(), updateCountDown).perform();
            }));
            UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message("radPullProgressTitle"),
                    updateCountDown, executingFlag);
            new Thread(ubt::queue).start();
        });
    }

}
