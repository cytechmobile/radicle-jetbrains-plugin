package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;

import java.util.List;

public class RadicleManagerListener implements ProjectManagerListener {

    private RadicleSettingsHandler radicleSettingsHandler;
    private static final Logger log = Logger.getInstance(RadicleManagerListener.class);

    @Override
    public void projectOpened(Project project) {
        var srv = project.getService(RadicleProjectService.class);
        showSuccessNotification(project);
        log.info("got service: " + srv);
    }

    private void showSuccessNotification(Project project) {
        radicleSettingsHandler = new RadicleSettingsHandler();
        var settings = radicleSettingsHandler.loadSettings();
        if (Strings.isNullOrEmpty(settings.getPath())) {
            BasicAction.showNotification(project, "radicle", "installedSuccessfully", NotificationType.INFORMATION,
                    List.of(new BasicAction.ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
        }
    }
}
