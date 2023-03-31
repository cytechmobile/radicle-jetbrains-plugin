package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadPath extends RadAction {

    public RadPath(Project project) {
        super(project);
    }

    @Override
    public ProcessOutput run() {
        var rad =  project.getService(RadicleProjectService.class);
        return rad.homePath();
    }

    @Override
    public String getActionName() {
        return "path";
    }
}
