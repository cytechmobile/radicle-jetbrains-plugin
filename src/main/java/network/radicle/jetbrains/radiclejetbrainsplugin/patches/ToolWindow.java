package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ToolWindow extends VcsToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        var contentManager = toolWindow.getContentManager();
        var issueContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), "Issues", true);
        var patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);



        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(com.intellij.openapi.wm.@NotNull ToolWindow shownToolWindow) {
                if (toolWindow == shownToolWindow && toolWindow.isVisible() && contentManager.isEmpty()) {
                    contentManager.addContent(patchContent);
                    contentManager.addContent(issueContent);
                    var controller = new PatchTabController(patchContent, project);
                    controller.createPatchesPanel();
                }
            }
        });
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> {
                    var gitRepoManager = GitRepositoryManager.getInstance(project);
                    var repos = gitRepoManager.getRepositories();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, false);
                        ApplicationManager.getApplication().invokeLater(() ->{
                            if (!radInitializedRepos.isEmpty()) {
                                toolWindow.setAvailable(true);
                            }
                        });
                    });
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return false;
    }
}
