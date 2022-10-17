package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.List;

public class RadPull implements RadAction {

    private GitRepository repo;

    public RadPull(GitRepository repo) {
        this.repo = repo;
    }

    @Override
    public ProcessOutput run () {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.pull(repo);
    }

    @Override
    public GitRepository getRepo() {
        return repo;
    }

    @Override
    public List<NotificationAction> notificationActions() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadPull");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadPull");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return RadicleBundle.message("radPullNotification");
    }

}
