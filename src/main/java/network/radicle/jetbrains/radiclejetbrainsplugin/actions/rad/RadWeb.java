package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadWeb extends RadAction {
    public RadWeb(GitRepository repo) {
        super(repo);
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.radWebJson(repo);
    }

    @Override
    public boolean shouldShowNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        return "Web";
    }

    @Override
    public boolean shouldUnlockIdentity() {
        return true;
    }
}
