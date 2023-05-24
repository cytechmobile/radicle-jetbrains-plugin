package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.testFramework.LightVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchProposalPanel;
import org.jetbrains.annotations.NotNull;

public class PatchVirtualFile extends LightVirtualFile {
    private final RadPatch patch;
    private final SingleValueModel<RadPatch> patchModel;
    private final PatchProposalPanel proposalPanel;

    public PatchVirtualFile(SingleValueModel<RadPatch> patch, PatchProposalPanel proposalPanel) {
        this.patch = patch.getValue();
        this.patchModel = patch;
        this.proposalPanel = proposalPanel;
    }

    @Override
    public @NlsSafe @NotNull String getName() {
        return patch.id;
    }

    @Override
    public @NotNull @NlsSafe String getPresentableName() {
        return patch.title;
    }

    public RadPatch getPatch() {
        return patch;
    }

    public SingleValueModel<RadPatch> getPatchModel() {
        return patchModel;
    }

    public PatchProposalPanel getProposalPanel() {
        return  proposalPanel;
    }
}
