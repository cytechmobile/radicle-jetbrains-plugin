package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadInit extends RadAction {
    private final String name;
    private final String description;
    private final String branch;

    public RadInit(GitRepository repo, String name, String description, String branch) {
        super(repo);
        this.name = name;
        this.description = description;
        this.branch = branch;
    }

    @Override
    public String getActionName() {
        return "Init";
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.init(repo, name, description, branch);
    }
}
