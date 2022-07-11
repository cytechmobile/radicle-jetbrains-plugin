package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

public class RadicleManagerListener implements ProjectManagerListener {

    private static final Logger log = Logger.getInstance(RadicleManagerListener.class);

    @Override
    public void projectOpened(Project project) {
        var srv = project.getService(RadicleProjectService.class);
        log.info("got service: " + srv);
    }
}
