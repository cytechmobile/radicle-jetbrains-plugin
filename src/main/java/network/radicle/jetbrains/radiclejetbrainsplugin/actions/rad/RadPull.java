package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.concurrent.CountDownLatch;

public class RadPull extends RadAction {
    public RadPull(GitRepository repo) {
        super(repo);
    }

    @Override
    public String getActionName() {
        return "Pull";
    }

    @Override
    public ProcessOutput run () {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.pull(repo);
    }
}
