package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor.IssueVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.TabController;

import java.util.Arrays;

public class IssueTabController extends TabController<RadIssue, IssueListSearchValue, IssueSearchPanelViewModel> {
    private final IssueListPanel issueListPanel;

    public IssueTabController(Content tab, Project project) {
        super(project, tab);
        issueListPanel = new IssueListPanel(this, project, this);
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

    public void createIssuePanel(RadIssue myIssue) {
        final var issueModel = new SingleValueModel<>(myIssue);
        createInternalIssuePanel(issueModel);
        issueModel.addListener(issue -> {
            var fetched = api.fetchIssue(myIssue.projectId, myIssue.repo, myIssue.id);
            if (fetched != null) {
                ApplicationManager.getApplication().invokeLater(() -> createIssuePanel(fetched));
            }
            return null;
        });
    }

    protected void createInternalIssuePanel(SingleValueModel<RadIssue> issue) {
        openIssueTimelineOnEditor(issue, true);
    }

    public void openIssueTimelineOnEditor(SingleValueModel<RadIssue> issueModel, boolean force) {
        var editorManager = FileEditorManager.getInstance(project);
        final var issue = issueModel.getValue();
        var file = new IssueVirtualFile(issueModel);
        var editorTabs = Arrays.stream(editorManager.getAllEditors()).filter(ed ->
                ed.getFile() instanceof IssueVirtualFile &&
                        ((IssueVirtualFile) ed.getFile()).getIssue().id.equals(issue.id)).toList();
        if (force) {
            for (var et : editorTabs) {
                editorManager.closeFile(et.getFile());
            }
        }

        if (force || editorTabs.isEmpty()) {
            editorManager.openFile(file, true);
        }
    }
}
