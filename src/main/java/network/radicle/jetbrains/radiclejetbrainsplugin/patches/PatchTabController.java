package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.TabController;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

public class PatchTabController extends TabController<RadPatch, PatchListSearchValue, PatchSearchPanelViewModel> {
    private final PatchListPanel patchListPanel;
    private SingleValueModel<RadPatch> patchModel;
    private PatchProposalPanel patchProposalPanel;
    private JComponent patchProposalJPanel;

    public PatchTabController(Content tab, Project project) {
        super(project, tab);
        patchListPanel = new PatchListPanel(this, project);
    }

    public void createPatchProposalPanel(RadPatch patch) {
        final var mainPanel = tab.getComponent();
        patchModel = new SingleValueModel<>(patch);
        createInternalPatchProposalPanel(patchModel, mainPanel);
        patchModel.addListener(p -> {
            var fetched = api.fetchPatch(patch.radProject.id, patch.repo, patch.id);
            if (fetched != null) {
                ApplicationManager.getApplication().invokeLater(() -> createPatchProposalPanel(fetched));
            }
            return null;
        });
    }

    public void createNewPatchPanel(List<GitRepository> gitRepos) {
        final var mainPanel = tab.getComponent();
        tab.setDisplayName(RadicleBundle.message("newPatch"));
        var createPanel = new CreatePatchPanel(this, project, gitRepos).create();
        mainPanel.removeAll();
        mainPanel.add(createPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    protected void createInternalPatchProposalPanel(SingleValueModel<RadPatch> patch, JComponent mainPanel) {
        tab.setDisplayName(RadicleBundle.message("patchProposalFrom") + ": " + patch.getValue().author.generateLabelText());
        patchProposalPanel = new PatchProposalPanel(this, patch);
        patchProposalJPanel = patchProposalPanel.createViewPatchProposalPanel();
        mainPanel.removeAll();
        mainPanel.add(patchProposalJPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        openPatchTimelineOnEditor(patch, patchProposalPanel, true);
    }

    public void openPatchTimelineOnEditor(SingleValueModel<RadPatch> myPatchModel, PatchProposalPanel proposalPanel, boolean force) {
        var editorManager = FileEditorManager.getInstance(project);
        final var patch = myPatchModel.getValue();
        var file = new PatchVirtualFile(myPatchModel, proposalPanel);
        var editorTabs = Arrays.stream(editorManager.getAllEditors()).filter(ed ->
                ed.getFile() instanceof PatchVirtualFile pvf && pvf.getPatch().id.equals(patch.id)).toList();
        if (force) {
            for (var et : editorTabs) {
                editorManager.closeFile(et.getFile());
            }
        }

        if (force || editorTabs.isEmpty()) {
            editorManager.openFile(file, true);
        }
    }

    public SingleValueModel<RadPatch> getPatchModel() {
        return patchModel;
    }

    @Override
    public String getTabName() {
        return RadicleBundle.message("patchTabName");
    }

    @Override
    public ListPanel<RadPatch, PatchListSearchValue, PatchSearchPanelViewModel> getPanel() {
        return patchListPanel;
    }

    public PatchListPanel getPatchListPanel() {
        return patchListPanel;
    }

    public PatchProposalPanel getPatchProposalPanel() {
        return patchProposalPanel;
    }

    public JComponent getPatchProposalJPanel() {
        return patchProposalJPanel;
    }
}
