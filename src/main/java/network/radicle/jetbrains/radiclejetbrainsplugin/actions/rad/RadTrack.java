package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadTrack implements RadAction {

    private String url;
    private GitRepository repo;

    public RadTrack(GitRepository repo, String url) {
        this.repo = repo;
        this.url = url;
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.track(repo,url);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadTrack");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadTrack");
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
