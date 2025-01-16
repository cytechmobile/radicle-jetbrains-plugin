package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;

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
        return rad.init(repo, name, description, branch, visibility);
    }
}
