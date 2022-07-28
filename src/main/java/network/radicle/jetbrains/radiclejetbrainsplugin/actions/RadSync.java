package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadSync implements RadAction {

    @Override
    public ProcessOutput run(GitRepository repo) {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        var output = rad.sync(repo);
        return output;
    }

    @Override
    public String getLoggerErrorMessage() {
        return "error in rad sync:";
    }

    @Override
    public String getLoggerSuccessMessage() {
        return "success in rad sync:";
    }

    @Override
    public String getNotificationSuccessMessage() {
        return "Synced project with radicle seed node";
    }
}
