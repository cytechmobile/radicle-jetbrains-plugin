package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPath extends RadAction {
    private String radPath;
    public RadPath(Project project, String radPath) {
        super(project);
        this.radPath = radPath;
    }

    @Override
    public ProcessOutput run() {
        var rad =  project.getService(RadicleProjectService.class);
        return rad.homePath(radPath);
    }

    @Override
    public String getActionName() {
        return "path";
    }
}
