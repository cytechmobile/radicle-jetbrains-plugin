package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPatchReview extends RadAction {
    private final String message;
    private final String verdict;
    private final String patchId;

    public RadPatchReview(GitRepository repo, String message, String verdict, String patchId) {
        super(repo);
        this.message = message;
        this.verdict = verdict;
        this.patchId = patchId;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.addReview(repo, verdict, message, patchId);
    }

    @Override
    public String getActionName() {
        return null;
    }
}
