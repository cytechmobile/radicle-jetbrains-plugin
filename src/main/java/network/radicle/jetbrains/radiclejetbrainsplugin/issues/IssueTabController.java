package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.TabController;

public class IssueTabController extends TabController<RadIssue, IssueListSearchValue, IssueSearchPanelViewModel> {
    private final IssueListPanel issueListPanel;

    public IssueTabController(Content tab, Project project) {
        super(project, tab);
        issueListPanel = new IssueListPanel(this, project);
    }

    public IssueListPanel getIssueListPanel() {
        return issueListPanel;
    }

    @Override
    public String getTabName() {
        return RadicleBundle.message("issues");
    }

    @Override
    public ListPanel<RadIssue, IssueListSearchValue, IssueSearchPanelViewModel> getPanel() {
        return issueListPanel;
    }

}
