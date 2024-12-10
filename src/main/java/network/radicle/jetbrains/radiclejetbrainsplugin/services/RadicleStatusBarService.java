package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadStatusBar;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.getInitializedReposWithNodeConfigured;

public class RadicleStatusBarService {
    private static final int PERIOD = 10;

    private final Project project;
    private Boolean isNodeRunning = null;
    private Boolean isHttpdRunning = null;
    private boolean isRadInitialized = false;

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
        var initializedRepos = getInitializedRepos();
        boolean previousRadInitialized = isRadInitialized;
        isRadInitialized = !initializedRepos.isEmpty();
        if (isRadInitialized != previousRadInitialized) {
            updateStatusBar();
        }
        if (!isRadInitialized) {
            return;
        }
        var settingsHandler = new RadicleProjectSettingsHandler(project);
        if (settingsHandler.isSettingsEmpty()) {
          /* Update the status bar and reset the node and httpd statuses to ensure
             that the status bar will be updated in the next run by checking the isFirstCheck() method */
            updateStatusBar();
            isNodeRunning = null;
            isHttpdRunning = null;
            return;
        }
        var nodeStatus = checkNodeStatus();
        // TODO: ignore httpd, moving to CLI only
        var httpdStatus = true; // checkHttpd(settingsHandler.loadSettings().getSeedNode());
        /* Update the status bar if this is the first time the checkServices method runs or if
        the node/httpd statuses have changed, and RAD is initialized: */
        if (isFirstCheck() || nodeStatus != isNodeRunning || httpdStatus != isHttpdRunning) {
            isNodeRunning = nodeStatus;
            isHttpdRunning = httpdStatus;
            updateStatusBar();
        }
    }

    public boolean isFirstCheck() {
        return isHttpdRunning == null && isNodeRunning == null;
    }

    public boolean checkNodeStatus() {
        var projectService = project.getService(RadicleProjectService.class);
        return projectService.isNodeRunning();
    }

    public boolean isNodeRunning() {
        return isNodeRunning != null && isNodeRunning;
    }

    public boolean isHttpdRunning() {
        return isHttpdRunning != null && isHttpdRunning;
    }

    public boolean isRadInitialized() {
        return isRadInitialized;
    }

    public List<GitRepository> getInitializedRepos() {
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        return getInitializedReposWithNodeConfigured(repos, false);
    }

    private void updateStatusBar() {
        //Update status bar availability (show / hide)
        project.getService(StatusBarWidgetsManager.class).updateWidget(RadStatusBar.class);
        //Update status bar icon and component
        WindowManager.getInstance().getStatusBar(project).updateWidget(RadStatusBar.ID);
    }
}
