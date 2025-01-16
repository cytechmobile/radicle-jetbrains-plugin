package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;

public class RadNodeStatus extends RadAction {
    private final String radPath;

    public RadNodeStatus(Project project, String radPath) {
        super(project);
        this.radPath = radPath;
    }

    @Override
    public ProcessOutput run() {
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
