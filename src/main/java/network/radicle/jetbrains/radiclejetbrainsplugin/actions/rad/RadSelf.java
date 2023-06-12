package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDetails;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadSelf extends RadAction {
    private final String radHome;
    private final String radPath;
    private boolean unlockIdentity = true;

    public RadSelf(String radHome, String radPath, Project project) {
        super(project);
        this.radHome = radHome;
        this.radPath = radPath;
    }

    public RadSelf(Project project) {
        super(project);
        var projectHandler = new RadicleProjectSettingsHandler(project);
        var projectSettings = projectHandler.loadSettings();
        this.radHome = projectSettings.getRadHome();
        this.radPath = projectSettings.getPath();
    }

    public void askForIdentity(boolean ask) {
        unlockIdentity = ask;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.self(radHome, radPath);
    }

    @Override
    public boolean shouldShowNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        return "Self";
    }

    public RadDetails getRadSelfDetails() {
        var output = this.perform();
        if (RadAction.isSuccess(output)) {
            return new RadDetails(output.getStdoutLines(true));
        }
        return null;
    }

    @Override
    public boolean shouldUnlockIdentity() {
        return unlockIdentity;
    }
}
