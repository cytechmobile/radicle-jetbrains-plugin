package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadPull;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadiclePullEvent extends AnAction {

    protected CountDownLatch updateCountDown;
    protected AtomicBoolean executingFlag = new AtomicBoolean(false);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();

        if (!BasicAction.isCliPathConfigured(e) || !BasicAction.hasGitRepos(e)) {
            return ;
        }

        this.updateCountDown = new CountDownLatch(repos.size());
        var pull = new RadPull();
        repos.forEach(repo -> ApplicationManager.getApplication().executeOnPooledThread(() ->
                new BasicAction(pull,repo,project,updateCountDown).perform()));

        UpdateBackgroundTask ubt = new UpdateBackgroundTask(project, RadicleBundle.message(pull.getProgressBarTitle()),
                updateCountDown,executingFlag);
        new Thread(ubt::queue).start();
    }
}
