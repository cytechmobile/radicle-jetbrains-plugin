package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.ListPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.TabController;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Arrays;

public class PatchTabController extends TabController {
    private final PatchListPanel patchListPanel;

    public PatchTabController(Content tab, Project project, ProjectApi myApi) {
        super(project, tab, myApi);
        patchListPanel = new PatchListPanel(this, project, myApi);
    }

    public void createPatchProposalPanel(RadPatch patch) {
        final var mainPanel = tab.getComponent();
        final var patchModel = new SingleValueModel<>(patch);
        createInternalPatchProposalPanel(patchModel, mainPanel);
        patchModel.addListener(p -> {
            var fetched = myApi.fetchPatch(patch.seedNode, patch.projectId, patch.repo, patch.id);
            if (fetched != null) {
                ApplicationManager.getApplication().invokeLater(() -> createPatchProposalPanel(fetched));
            }
            return null;
        });
    }

    protected void createInternalPatchProposalPanel(SingleValueModel<RadPatch> patch, JComponent mainPanel) {
        tab.setDisplayName("Patch Proposal from: " + patch.getValue().author);
        var patchProposalPanel = new PatchProposalPanel(this, patch);
        var panel = patchProposalPanel.createViewPatchProposalPanel();
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        openPatchTimelineOnEditor(patch, patchProposalPanel, true);
    }

    public void openPatchTimelineOnEditor(SingleValueModel<RadPatch> patchModel, PatchProposalPanel proposalPanel, boolean force) {
        var editorManager = FileEditorManager.getInstance(project);
        final var patch = patchModel.getValue();
        var file = new PatchVirtualFile(patchModel, proposalPanel);
        var editorTabs = Arrays.stream(editorManager.getAllEditors()).filter(ed ->
                ed.getFile() instanceof PatchVirtualFile &&
                        ((PatchVirtualFile) ed.getFile()).getPatch().id.equals(patch.id)).toList();
        if (force) {
            for (var et : editorTabs) {
                editorManager.closeFile(et.getFile());
            }
        }

        if (force || editorTabs.isEmpty()) {
            editorManager.openFile(file, true);
        }
    }

    @Override
    public String getTabName() {
        return RadicleBundle.message("patchTabName");
    }

    @Override
    public ListPanel getPanel() {
        return patchListPanel;
    }

    public Disposable getDisposer() {
        return tab.getDisposer();
    }

    public PatchListPanel getPatchListPanel() {
        return patchListPanel;
    }
}
