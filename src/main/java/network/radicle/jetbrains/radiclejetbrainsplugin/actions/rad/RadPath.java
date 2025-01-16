package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;

public class RadPath extends RadAction {
    private String radPath;
    public RadPath(Project project, String radPath) {
        super(project);
        this.radPath = radPath;
    }

    @Override
    public ProcessOutput run() {
        return rad.homePath(radPath);
    }

    @Override
    public String getActionName() {
        return "path";
    }
}
