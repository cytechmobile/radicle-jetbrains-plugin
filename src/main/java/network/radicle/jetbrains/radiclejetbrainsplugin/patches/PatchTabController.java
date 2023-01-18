package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;

import java.awt.BorderLayout;

public class PatchTabController {
    private Project project;
    private Content tab;
    private PatchListPanel patchListPanel;
    private PatchProposalPanel patchProposalPanel;

    public PatchTabController(Content tab, Project project) {
        this.tab = tab;
        this.project = project;
    }

    public void createPatchesPanel() {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        patchListPanel = new PatchListPanel(this, project);
        var createdPanel = patchListPanel.create();
        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        mainPanel.add(createdPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void createPatchProposalPanel(RadPatch patch) {
        tab.setDisplayName("Patch Proposal from: " + patch.peerId);
        patchProposalPanel = new PatchProposalPanel();
        var panel = patchProposalPanel.createViewPatchProposalPanel(this, patch, project);
        var mainPanel = tab.getComponent();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public Disposable getDisposer() {
        return tab.getDisposer();
    }

    public PatchListPanel getPatchListPanel() {
        return patchListPanel;
    }

    public PatchProposalPanel getPatchProposalPanel() {
        return patchProposalPanel;
    }
}
