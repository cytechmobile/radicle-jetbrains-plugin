package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadNodeStatus extends RadAction {
    private final String radPath;

    public RadNodeStatus(Project project, String radPath) {
        super(project);
        this.radPath = radPath;
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.checkNodeStatus(radPath);
    }

    @Override
    public boolean shouldShowNotification() {
        return false;
    }

    @Override
    public String getActionName() {
        return "NodeStatus";
    }
}
