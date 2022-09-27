package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadPush implements RadAction {

    private GitRepository repo;

    @Override
    public ProcessOutput run () {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.push(repo);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadPush");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadPush");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return RadicleBundle.message("radPushNotification");
    }

    @Override
    public GitRepository getRepo() {
        return repo;
    }
}
