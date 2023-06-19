package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueTabController;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchTabController;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.List;

public class RadicleToolWindow extends VcsToolWindowFactory {
    public ToolWindowManagerListener toolWindowManagerListener;
    public PatchTabController patchTabController;
    public IssueTabController issueTabController;

    @Override
    public void init(@NotNull ToolWindow window) {
        super.init(window);
        //Workaround at activating toolwindow content and check for available radicle repos
        window.getContentManager();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        var contentManager = toolWindow.getContentManager();
        var issueContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);
        var patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);
        patchContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadiclePatchProposalsContent"));
        issueContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadicleIssueContent"));
        toolWindowManagerListener = new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                if (toolWindow == shownToolWindow && toolWindow.isVisible() && contentManager.isEmpty()) {
                    contentManager.addContent(patchContent);
                    contentManager.addContent(issueContent);
                    contentManager.setSelectedContent(patchContent, true);
                    patchTabController = new PatchTabController(patchContent, project);
                    patchTabController.createPanel();
                    issueTabController = new IssueTabController(issueContent, project);
                    issueTabController.createPanel();
                }
            }
        };

        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, toolWindowManagerListener);
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> {
                    //var gitRepoManager = GitRepositoryManager.getInstance(project);
                    //var repos = gitRepoManager.getRepositories();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInited = RadAction.getInitializedReposWithNodeConfigured(List.of(repository), false);
                        if (!radInited.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                toolWindow.setAvailable(true);
                            });
                        }
                    });
                    /*ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, false);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!radInitializedRepos.isEmpty()) {
                                toolWindow.setAvailable(true);
                            }
                        });
                    });*/
                });
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return false;
    }
}
