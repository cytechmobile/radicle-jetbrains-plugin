package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadInspect extends RadAction {

    public RadInspect(GitRepository repo) {
        super(repo);
    }

    @Override
    public ProcessOutput run() {
        var rad = repo.getProject().getService(RadicleProjectService.class);
        return rad.inspect(repo);
    }

    @Override
    public String getActionName() {
        return "Inspect";
    }
}
