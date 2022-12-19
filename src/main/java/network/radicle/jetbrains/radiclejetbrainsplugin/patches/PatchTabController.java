package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import git4idea.changes.GitChangeUtils;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadRemote;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class PatchTabController {
    private Project project;
    private Content tab;

    public PatchTabController(Content tab, Project project) {
        this.tab = tab;
        this.project = project;
    }

    public void createPatchesPanel()  {
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
        if (patch.changes == null) {
            patch.changes = new ArrayList<>();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                //first make sure that the peer is tracked
                boolean error = checkAndTrackPeerIfNeeded(patch);
                if (error) {
                    createPatchesPanel();
                    return;
                }
                var computedChanges = GitChangeUtils.getDiff(patch.repo, patch.commitHash, patch.repo.getCurrentRevision(), true);
                System.out.println("Calculated changes for patch: " + patch + " : " + computedChanges);
                patch.changes = computedChanges == null ? Collections.emptyList() : new ArrayList<>(computedChanges);
                ApplicationManager.getApplication().invokeLater(() -> this.createPatchProposalPanel(patch));
            });
            return;
        }
        tab.setDisplayName("Patch Proposal from: " + patch.peerId);
        var patchProposalViewPanel = new PatchProposalPanel().createViewPatchProposalPanel(this, patch);
        var mainPanel = tab.getComponent();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.removeAll();
        mainPanel.add(patchProposalViewPanel,BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    protected boolean checkAndTrackPeerIfNeeded(RadPatch patch) {
        if (patch.self) {
            return false;
        }
        final var trackedPeers = new RadRemote(patch.repo).findTrackedPeers();
        if (trackedPeers != null && !trackedPeers.isEmpty()) {
            var tracked = trackedPeers.stream().filter(p -> p.id().equals(patch.peerId)).findAny().orElse(null);
            if (tracked != null) {
                return false;
            }
        }

        var trackPeer = new RadTrack(patch.repo, new RadTrack.Peer(patch.peerId));
        var out = trackPeer.perform();
        if (!RadTrack.isSuccess(out)) {
            return true;
        }
        return false;
    }
}
