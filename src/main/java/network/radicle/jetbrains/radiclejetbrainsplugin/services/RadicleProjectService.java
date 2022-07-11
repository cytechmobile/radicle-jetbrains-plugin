package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

public class RadicleProjectService {

    public RadicleProjectService(Project project) {
        System.out.println(RadicleBundle.message("projectService", project.getName()));
    }
}
