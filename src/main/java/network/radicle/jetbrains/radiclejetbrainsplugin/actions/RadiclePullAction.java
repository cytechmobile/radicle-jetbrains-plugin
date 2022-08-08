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

        if (!BasicAction.isCliPathConfigured(project) || !BasicAction.hasGitRepos(project) ||
                !BasicAction.isSeedNodeConfigured(project)) {
            return ;
        }

        this.updateCountDown = new CountDownLatch(repos.size());
        var pull = new RadPull();
        repos.forEach(repo -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                new BasicAction(pull, repo, project, updateCountDown).perform()));

        UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message(pull.getProgressBarTitle()),
                updateCountDown, executingFlag);
        new Thread(ubt::queue).start();
    }

    public CountDownLatch getUpdateCountDown() {
        return updateCountDown;
    }
}
