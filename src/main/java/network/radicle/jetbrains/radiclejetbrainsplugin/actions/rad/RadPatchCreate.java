package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPatchCreate extends RadAction {
    private final String title;
    private final String description;
    private final String branch;

    public RadPatchCreate(GitRepository repo, String title, String description, String branch) {
        super(repo);
        this.title = title;
        this.description = description;
        this.branch = branch;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.createPatch(repo, title, description, branch);
    }

    @Override
    public String getActionName() {
        return null;
    }
}
