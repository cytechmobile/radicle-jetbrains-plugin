package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPatchComment extends RadAction {
    private final String patchId;
    public final String message;

    public RadPatchComment(GitRepository repo, String patchId, String message) {
        super(repo);
        this.patchId = patchId;
        this.message = message;
    }

    @Override
    public String getActionName() {
        return "PatchComment";
    }

    @Override
    public boolean shouldShowNotification() {
        return true;
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.patchComment(repo, patchId, message);
    }
}
