package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

import java.util.List;

public class RadInit implements RadAction {
    private final GitRepository repo;
    private final String name;
    private final String description;
    private final String branch;

    public RadInit (GitRepository repo, String name, String description, String branch) {
        this.repo = repo;
        this.name = name;
        this.description = description;
        this.branch = branch;
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.init(repo,name,description,branch);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("initError");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("initSuccess");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return "";
    }

    @Override
    public GitRepository getRepo() {
        return repo;
    }

}
