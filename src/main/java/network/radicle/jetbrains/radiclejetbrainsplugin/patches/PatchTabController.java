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

    public PatchTabController(Content tab, Project project) {
        this.tab = tab;
        this.project = project;
    }

    public void createPatchesPanel() {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        var panel = new PatchListPanel(this, project);
        var createdPanel = panel.create();
        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        mainPanel.add(createdPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void createPatchProposalPanel(RadPatch patch) {
        tab.setDisplayName("Patch Proposal from: " + patch.peerId);
        var patchProposalViewPanel = new PatchProposalPanel().createViewPatchProposalPanel(this, patch, project);
        var mainPanel = tab.getComponent();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.removeAll();
        mainPanel.add(patchProposalViewPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public Disposable getDisposer() {
        return tab.getDisposer();
    }
}
