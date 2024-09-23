package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.util.List;

public class RadPatchLabel extends RadAction {
    private final String patchId;
    private final List<String> addedLabels;
    private final List<String> deleteLabels;

    public RadPatchLabel(GitRepository repo, String patchId, List<String> addedLabels, List<String> deleteLabels) {
        super(repo);
        this.patchId = patchId;
        this.addedLabels = addedLabels;
        this.deleteLabels = deleteLabels;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.addRemovePatchLabels(repo, patchId, addedLabels, deleteLabels);
    }

    @Override
    public String getActionName() {
        return "Label";
    }
}
