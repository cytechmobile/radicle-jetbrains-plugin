package network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;


public class RadClone extends RadAction {
    private String radUrn;
    private String directory;
    private String radPath;
    private String radHome;

    public RadClone(String radUrn, String directory, String radHome, String radPath, Project project) {
        super(project);
        this.radUrn = radUrn;
        this.directory = directory;
        this.radHome = radHome;
        this.radPath = radPath;
    }

    @Override
    public String getActionName() {
        return "Clone";
    }

    @Override
    public ProcessOutput run() {
        var rad = project.getService(RadicleProjectService.class);
        return rad.clone(radUrn, directory, this.radPath, this.radHome);
    }
}
