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

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Arrays;

public class IssueTabController extends TabController<RadIssue, IssueListSearchValue, IssueSearchPanelViewModel> {
    private final IssueListPanel issueListPanel;
    private SingleValueModel<RadIssue> myIssueModel;
    private JComponent issueJPanel;
    private IssuePanel issuePanel;

    public IssueTabController(Content tab, Project project) {
        super(project, tab);
        issueListPanel = new IssueListPanel(this, project);
    }

    public IssueListPanel getIssueListPanel() {
        return issueListPanel;
    }

    public SingleValueModel<RadIssue> getIssueModel() {
        return myIssueModel;
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
        final var mainPanel = tab.getComponent();
        final var issueModel = new SingleValueModel<>(myIssue);
        this.myIssueModel = issueModel;
        createInternalIssuePanel(issueModel, mainPanel);
        issueModel.addListener(issue -> {
            var fetched = api.fetchIssue(myIssue.projectId, myIssue.repo, myIssue.id);
            // Reload whole panel
            issueListPanel.updateListPanel();
            if (fetched != null) {
                ApplicationManager.getApplication().invokeLater(() -> createIssuePanel(fetched));
            }
            return null;
        });
    }

    public void createNewIssuePanel() {
        final var mainPanel = tab.getComponent();
        tab.setDisplayName(RadicleBundle.message("newIssue"));

        var createPanel = new CreateIssuePanel(this, project).create();
        mainPanel.removeAll();
        mainPanel.add(createPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    protected void createInternalIssuePanel(SingleValueModel<RadIssue> issue, JComponent mainPanel) {
        tab.setDisplayName("Issue : " + issue.getValue().id.substring(0,  Math.min(7, issue.getValue().id.length())));
        issuePanel = new IssuePanel(this, issue);
        issueJPanel = issuePanel.createPanel();
        mainPanel.removeAll();
        mainPanel.add(issueJPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        openIssueTimelineOnEditor(issue, true);
    }

    public IssuePanel getIssuePanel() {
        return issuePanel;
    }

    public JComponent getIssueJPanel() {
        return issueJPanel;
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
