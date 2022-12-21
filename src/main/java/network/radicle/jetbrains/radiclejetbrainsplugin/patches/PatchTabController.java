package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import git4idea.changes.GitChangeUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadRemote;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class PatchTabController {
    private Project project;
    private Content tab;
    private CoroutineScope scope;

    public PatchTabController(Content tab, Project project, CoroutineScope scope) {
        this.tab = tab;
        this.project = project;
        this.scope = scope;
    }

    public void createPatchesPanel()  {
        tab.setDisplayName(RadicleBundle.message("patchTabName"));
        var mainPanel = tab.getComponent();
        var panel = new PatchListPanel(this, project, scope);
        var createdPanel = panel.create();
        mainPanel.setLayout(new BorderLayout(5, 10));
        mainPanel.removeAll();
        mainPanel.add(createdPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void createPatchProposalPanel(RadPatch patch) {
        tab.setDisplayName("Patch Proposal from: " + patch.peerId);
        var patchProposalViewPanel = new PatchProposalPanel().createViewPatchProposalPanel(this, patch);
        var mainPanel = tab.getComponent();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.removeAll();
        mainPanel.add(patchProposalViewPanel,BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public Disposable getDisposer() {
        return tab.getDisposer();
    }
}
