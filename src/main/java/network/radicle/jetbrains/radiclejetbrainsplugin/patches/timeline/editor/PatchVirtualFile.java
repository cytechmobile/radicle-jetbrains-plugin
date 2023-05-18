package network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.testFramework.LightVirtualFile;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;

public class PatchVirtualFile extends LightVirtualFile {
    private final RadPatch patch;
    private final SingleValueModel<RadPatch> patchModel;

    public PatchVirtualFile(SingleValueModel<RadPatch> patch) {
        this.patch = patch.getValue();
        this.patchModel = patch;
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
}
