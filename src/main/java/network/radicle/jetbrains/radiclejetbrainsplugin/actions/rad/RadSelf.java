package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadSelf extends RadAction {
    private String radHome;
    private String radPath;

    public RadSelf(String radHome, String radPath, Project project) {
        super(project);
        this.radHome = radHome;
        this.radPath = radPath;
    }

    public RadSelf(Project project) {
        super(project);
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
}
