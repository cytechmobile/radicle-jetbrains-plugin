package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.List;

public class RadicleToolWindow extends VcsToolWindowFactory {
    protected ToolWindowManagerListener toolWindowManagerListener;
    protected PatchTabController patchTabController;
    protected ProjectApi myApi;

    public RadicleToolWindow() {
        myApi = new ProjectApi();
    }

    public RadicleToolWindow(ProjectApi api) {
        myApi = api;
    }

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
        var issueContent = toolWindow.getContentManager().getFactory().createContent(getIssuePanel(), "Issues", true);
        var patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);
        patchContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadiclePatchProposalsContent"));
        toolWindowManagerListener = new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                if (toolWindow == shownToolWindow && toolWindow.isVisible() && contentManager.isEmpty()) {
                    contentManager.addContent(patchContent);
                    contentManager.addContent(issueContent);
                    contentManager.setSelectedContent(patchContent, true);
                    patchTabController = new PatchTabController(patchContent, project, myApi);
                    patchTabController.createPatchesPanel();
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

    private JComponent getIssuePanel() {
        var mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JLabel(RadicleBundle.message("issueContent"), SwingConstants.CENTER), BorderLayout.CENTER);
        return mainPanel;
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return false;
    }
}
