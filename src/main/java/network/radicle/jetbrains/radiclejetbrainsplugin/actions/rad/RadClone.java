package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;

public class RadClone implements RadAction {
    private String radUrn;
    private String directory;

    public RadClone(String radUrn, String directory) {
        this.radUrn = radUrn;
        this.directory = directory;
    }

    @Override
    public ProcessOutput run() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        return rad.clone(radUrn,directory);
    }

    @Override
    public String getErrorMessage() {
        return RadicleBundle.message("errorInRadClone");
    }

    @Override
    public String getSuccessMessage() {
        return RadicleBundle.message("successInRadClone");
    }

    @Override
    public String getNotificationSuccessMessage() {
        return "";
    }

    @Override
    public GitRepository getRepo() {
        return null;
    }
}
