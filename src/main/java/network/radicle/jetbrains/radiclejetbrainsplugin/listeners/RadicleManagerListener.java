package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadicleManagerListener implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        showSuccessNotification(project);
    }

    private void showSuccessNotification(Project project) {
        var radicleSettingsHandler = new RadicleProjectSettingsHandler(project);
        var settings = radicleSettingsHandler.loadSettings();
        if (Strings.isNullOrEmpty(settings.getPath())) {
            RadAction.showNotification(project, "radicle", "installedSuccessfully", NotificationType.INFORMATION,
                    List.of(new RadAction.ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
        }
    }
}
