package network.radicle.jetbrains.radiclejetbrainsplugin.patches.overview;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.testFramework.LightVirtualFile;
import git4idea.GitCommit;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PatchVirtualFile extends LightVirtualFile {
    private final RadPatch patch;
    private final SingleValueModel<List<GitCommit>> commits;

    public PatchVirtualFile(RadPatch patch, SingleValueModel<List<GitCommit>> commits) {
        this.patch = patch;
        this.commits = commits;
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


    public SingleValueModel<List<GitCommit>> getCommits() {
        return commits;
    }
}
