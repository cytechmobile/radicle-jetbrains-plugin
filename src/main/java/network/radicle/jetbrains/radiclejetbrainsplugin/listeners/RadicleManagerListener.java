package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadicleManagerListener implements StartupActivity {
    private static final Logger log = Logger.getInstance(RadicleManagerListener.class);

    private void showSuccessNotification(Project project) {
        var radicleSettingsHandler = new RadicleSettingsHandler();
        var settings = radicleSettingsHandler.loadSettings();
        if (Strings.isNullOrEmpty(settings.getPath())) {
            RadAction.showNotification(project, "radicle", "installedSuccessfully", NotificationType.INFORMATION,
                    List.of(new RadAction.ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
        }
    }
    @Override
    public void runActivity(@NotNull Project project) {
        var srv = project.getService(RadicleProjectService.class);
        showSuccessNotification(project);
        log.info("got service: " + srv);
    }
}
