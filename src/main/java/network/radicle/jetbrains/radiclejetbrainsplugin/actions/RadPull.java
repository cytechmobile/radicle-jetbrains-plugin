package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadPull implements RadAction {

    @Override
    public ProcessOutput run (GitRepository repo) {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output = rad.pull(repo);
        return output;
    }

    @Override
    public String getLoggerErrorMessage() {
        return "error in rad pull:";
    }

    @Override
    public String getLoggerSuccessMessage() {
        return "success in rad pull:";
    }

    @Override
    public String getNotificationSuccessMessage() {
        return "Remotes fetched";
    }
}
