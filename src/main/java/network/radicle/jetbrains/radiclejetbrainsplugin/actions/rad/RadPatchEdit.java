package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPatchEdit extends RadAction {
    private final String patchId;
    public final String title;
    public final String description;

    public RadPatchEdit(GitRepository repo, String patchId, String title, String description) {
        super(repo);
        this.patchId = patchId;
        this.title = title;
        this.description = description;
    }

    @Override
    public String getActionName() {
        return "PatchEdit";
    }

    @Override
    public boolean shouldShowNotification() {
        return true;
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.patchEdit(repo, patchId, title, description);
    }
}
