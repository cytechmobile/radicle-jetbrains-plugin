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
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class RadicleToolWindow extends VcsToolWindowFactory {

    protected ToolWindowManagerListener toolWindowManagerListener;
    protected PatchTabController patchTabController;
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
                    patchTabController = new PatchTabController(patchContent, project);
                    patchTabController.createPatchesPanel();
                }
            }
        };

        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, toolWindowManagerListener);
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> {
                    var gitRepoManager = GitRepositoryManager.getInstance(project);
                    var repos = gitRepoManager.getRepositories();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, false);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!radInitializedRepos.isEmpty()) {
                                toolWindow.setAvailable(true);
                            }
                        });
                    });
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
