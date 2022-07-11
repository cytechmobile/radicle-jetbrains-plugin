package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;

public class RadicleProjectService {

    private static final Logger log = Logger.getInstance(RadicleProjectService.class);

    public RadicleProjectService(Project project) {
        log.info(RadicleBundle.message("projectService", project.getName()));
    }
}
