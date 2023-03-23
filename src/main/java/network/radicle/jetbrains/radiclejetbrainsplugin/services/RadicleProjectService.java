package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RadicleProjectService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RadicleProjectService.class);
    private static final String GIT_PUSH_GROUP_ID = "Vcs Notifications";

    private Project project = null;
    public List<PushInfo> pushDetails;
    public boolean forceRadPush;

    public RadicleProjectService(Project project) {
        logger.info(RadicleBundle.message("projectService", project.getName()));
        this.project = project;
    }
}
