package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadSync implements RadAction {

    private GitRepository repo;

    public RadSync(GitRepository repo) {
        this.repo = repo;
    }

    @Override
    public ProcessOutput run () {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.sync(repo);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadSync");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadSync");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return RadicleBundle.message("radSyncNotification");
    }


    @Override
    public GitRepository getRepo() {
        return repo;
    }
}
