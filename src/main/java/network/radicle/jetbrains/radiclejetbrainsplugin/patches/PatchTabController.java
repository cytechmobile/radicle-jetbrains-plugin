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

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Arrays;

public class PatchTabController {
    private Project project;
    private Content tab;
    private PatchListPanel patchListPanel;
    private PatchProposalPanel patchProposalPanel;
    private ProjectApi myApi;

    public PatchTabController(Content tab, Project project, ProjectApi myApi) {
        this.tab = tab;
        this.project = project;
        this.myApi = myApi;
    }

    public void createPatchesPanel() {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        patchListPanel = new PatchListPanel(this, project, myApi);
        var createdPanel = patchListPanel.create();
        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        mainPanel.add(createdPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
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
        patchProposalPanel = new PatchProposalPanel(this, patch);
        var panel = patchProposalPanel.createViewPatchProposalPanel();
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        openPatchTimelineOnEditor(patch, true);
    }

    public void openPatchTimelineOnEditor(SingleValueModel<RadPatch> patchModel, boolean force) {
        var editorManager = FileEditorManager.getInstance(project);
        final var patch = patchModel.getValue();
        var file = new PatchVirtualFile(patchModel);
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

    public Disposable getDisposer() {
        return tab.getDisposer();
    }

    public PatchListPanel getPatchListPanel() {
        return patchListPanel;
    }
}
