package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ToolWindow extends VcsToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        var contentManager = toolWindow.getContentManager();
        var issueContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), "Issues", true);
        var patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);
        contentManager.addContent(patchContent);
        contentManager.addContent(issueContent);
        var controller = new PatchTabController(patchContent, project);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }
}
