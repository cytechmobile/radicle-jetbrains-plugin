package network.radicle.jetbrains.radiclejetbrainsplugin.listeners;

import com.google.common.base.Strings;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadicleManagerListener implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        showSuccessNotification(project);
        return null;
    }

    private void showSuccessNotification(Project project) {
        var radicleSettingsHandler = new RadicleProjectSettingsHandler(project);
        var settings = radicleSettingsHandler.loadSettings();
        var radPath = settings.getPath();
        var radHome = settings.getRadHome();
        if (Strings.isNullOrEmpty(radPath)) {
            radPath = detectRadPath(project);
            radicleSettingsHandler.savePath(radPath);
        }
        if (!Strings.isNullOrEmpty(radPath) && Strings.isNullOrEmpty(radHome)) {
            radHome = detectRadHome(project, radPath);
            radicleSettingsHandler.saveRadHome(radHome);
        }
        if (Strings.isNullOrEmpty(radPath) || Strings.isNullOrEmpty(radHome)) {
            RadAction.showNotification(project, "radicle", "installedSuccessfully", NotificationType.INFORMATION,
                    List.of(new RadAction.ConfigureRadCliNotificationAction(project, RadicleBundle.lazyMessage("configure"))));
        }
    }

    public String detectRadPath(Project project) {
        return project.getService(RadicleProjectService.class).detectRadPath();
    }

    public String detectRadHome(Project project, String radPath) {
        return project.getService(RadicleProjectService.class).detectRadHome(radPath);
    }

}
