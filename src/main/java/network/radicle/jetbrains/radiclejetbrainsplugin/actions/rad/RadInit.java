package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadInit extends RadAction {
    private final String name;
    private final String description;
    private final String branch;
    private final String visibility;

    public RadInit(GitRepository repo, String name, String description, String branch, String visibility) {
        super(repo);
        this.name = name;
        this.description = description;
        this.branch = branch;
        this.visibility = visibility;
    }

    @Override
    public String getActionName() {
        return "Init";
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.init(repo, name, description, branch, visibility);
    }
}
