package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadStatusBar;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RadicleStatusBarService {
    private static final int PERIOD = 10;

    private final Project project;
    private boolean isNodeRunning = false;
    private boolean isHttpdRunning = false;
    private boolean initialCheck = false;

    public RadicleStatusBarService(Project myProject) {
        this.project = myProject;
        checkServicesStatus(PERIOD);
    }

    public void checkServicesStatus(int period) {
        var exec = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().
                setDaemon(true).setNameFormat("RadicleStatusBarService-%d").build());
        exec.scheduleAtFixedRate(this::checkServices, 0, period, TimeUnit.SECONDS);
    }

    protected void checkServices() {
        var settingsHandler = new RadicleProjectSettingsHandler(project);
        if (settingsHandler.isSettingsEmpty()) {
            WindowManager.getInstance().getStatusBar(project).updateWidget(RadStatusBar.ID);
            initialCheck = false;
            return;
        }
        var nodeStatus = checkNodeStatus();
        var httpdStatus = checkHttpd(settingsHandler.loadSettings().getSeedNode());
        if (!initialCheck || (nodeStatus != isNodeRunning || httpdStatus != isHttpdRunning)) {
            initialCheck = true;
            isNodeRunning = nodeStatus;
            isHttpdRunning = httpdStatus;
            WindowManager.getInstance().getStatusBar(project).updateWidget(RadStatusBar.ID);
        }
    }

    public boolean checkNodeStatus() {
        var projectService = project.getService(RadicleProjectService.class);
        return projectService.isNodeRunning();
    }

    public boolean checkHttpd(SeedNode seedNode) {
        var api = project.getService(RadicleProjectApi.class);
        return Utils.isValidNodeApi(api.checkApi(seedNode, false));
    }

    public boolean isNodeRunning() {
        return isNodeRunning;
    }

    public boolean isHttpdRunning() {
        return isHttpdRunning;
    }
}
