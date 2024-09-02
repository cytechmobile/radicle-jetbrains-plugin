package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.util.ui.UIUtil;
import git4idea.repo.GitRepository;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleStatusBarService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadStatusBar;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.util.List;

@RunWith(JUnit4.class)
public class StatusPanelTest extends AbstractIT {
    private RadicleProjectSettingsHandler radicleProjectSettingsHandler;

    @Before
    public void beforeTest() {
        radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(getProject());
    }

    @Test
    public void testStatusPanelWithMissingSettings() {
        radicleProjectSettingsHandler.savePath("");
        var statusBarServiceStub = new StatusBarServiceStub(getProject(), true, false);
        stubStatusBarService(statusBarServiceStub);
        var radStatusBar = new RadStatusBar();
        var widget = (RadStatusBar.RadWidget) radStatusBar.createWidget(getProject(), new CoroutineScope() {
            @NotNull
            @Override
            public CoroutineContext getCoroutineContext() {
                return null;
            }
        });
        var panel = widget.createPanel();
        var label = UIUtil.findComponentOfType(panel, JLabel.class);
        var button = UIUtil.findComponentOfType(panel, JButton.class);
        assertNotNull(label);
        assertNotNull(button);
        assertTrue(radStatusBar.isAvailable(getProject()));
        assertEquals(widget.getIcon().toString(), RadicleIcons.RADICLE_STATUS_BAR_MISSING_SETTINGS.toString());
        assertEquals(label.getText(), RadicleBundle.message("missing.settings"));
        assertEquals(button.getText(), RadicleBundle.message("configure"));
    }

    @Test
    public void testStatusPanelWithServicesRunning() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("/mypath");
        var statusBarServiceStub = new StatusBarServiceStub(getProject(), true, true);
        stubStatusBarService(statusBarServiceStub);
        var radStatusBar = new RadStatusBar();
        var widget = (RadStatusBar.RadWidget) radStatusBar.createWidget(getProject(), new CoroutineScope() {
            @NotNull
            @Override
            public CoroutineContext getCoroutineContext() {
                return null;
            }
        });
        statusBarServiceStub.waitForStatusBarToUpdate();
        var panel = widget.createPanel();
        var nodeLabel = UIUtil.findComponentOfType((JPanel) ((JPanel) panel.getComponents()[1]).getComponents()[0], JLabel.class);
        var httpdLabel = UIUtil.findComponentOfType((JPanel) ((JPanel) panel.getComponents()[1]).getComponents()[1], JLabel.class);
        assertTrue(radStatusBar.isAvailable(getProject()));
        assertEquals(nodeLabel.getText(), RadicleBundle.message("radicle.node"));
        assertEquals(httpdLabel.getText(), RadicleBundle.message("radicle.httpd"));
        assertEquals(nodeLabel.getIcon().toString(), AllIcons.RunConfigurations.TestPassed.toString());
        assertEquals(httpdLabel.getIcon().toString(), AllIcons.RunConfigurations.TestPassed.toString());
        assertEquals(widget.getIcon().toString(), RadicleIcons.RADICLE_STATUS_BAR_SERVICES_RUNNING.toString());
    }

    @Test
    public void testStatusPanelWithServicesNotRunning() throws InterruptedException {
        radicleProjectSettingsHandler.savePath("/mypath");
        var statusBarServiceStub = new StatusBarServiceStub(getProject(), true, false);
        stubStatusBarService(statusBarServiceStub);
        var radStatusBar = new RadStatusBar();
        var widget = (RadStatusBar.RadWidget) radStatusBar.createWidget(getProject(), new CoroutineScope() {
            @NotNull
            @Override
            public CoroutineContext getCoroutineContext() {
                return null;
            }
        });
        statusBarServiceStub.waitForStatusBarToUpdate();
        var panel = widget.createPanel();
        var nodeLabel = UIUtil.findComponentOfType((JPanel) ((JPanel) panel.getComponents()[1]).getComponents()[0], JLabel.class);
        var httpdLabel = UIUtil.findComponentOfType((JPanel) ((JPanel) panel.getComponents()[1]).getComponents()[1], JLabel.class);
        assertTrue(radStatusBar.isAvailable(getProject()));
        assertEquals(nodeLabel.getText(), RadicleBundle.message("radicle.node"));
        assertEquals(httpdLabel.getText(), RadicleBundle.message("radicle.httpd"));
        assertEquals(nodeLabel.getIcon().toString(), AllIcons.RunConfigurations.TestPassed.toString());
        assertEquals(httpdLabel.getIcon().toString(), AllIcons.RunConfigurations.TestError.toString());
        assertEquals(widget.getIcon().toString(), RadicleIcons.RADICLE_STATUS_BAR_SERVICES_NOT_RUNNING.toString());
    }

    @Test
    public void testStatusPanelWithoutRadInitialized() {
        var statusBarServiceStub = new StatusBarServiceStub(getProject(), true, false) {
            @Override
            public List<GitRepository> getInitializedRepos() {
                return List.of();
            }
        };
        stubStatusBarService(statusBarServiceStub);
        var radStatusBar = new RadStatusBar();
        var widget = (RadStatusBar.RadWidget) radStatusBar.createWidget(getProject(), new CoroutineScope() {
            @NotNull
            @Override
            public CoroutineContext getCoroutineContext() {
                return null;
            }
        });
        widget.createPanel();
        assertFalse(radStatusBar.isAvailable(getProject()));
    }

    public class StatusBarServiceStub extends RadicleStatusBarService {
        private final boolean isNodeRunning;
        private final boolean isHttpdRunning;

        public StatusBarServiceStub(Project project, boolean isNodeRunning, boolean isHttpdRunning) {
            super(project);
            this.isNodeRunning = isNodeRunning;
            this.isHttpdRunning = isHttpdRunning;
        }

        @Override
        public void checkServicesStatus(int period) {
            super.checkServicesStatus(500);
        }

        @Override
        public boolean checkNodeStatus() {
            return isNodeRunning;
        }

        @Override
        public boolean checkHttpd(SeedNode seedNode) {
            return isHttpdRunning;
        }

        public void waitForStatusBarToUpdate() throws InterruptedException {
            while (isFirstCheck()) {
                Thread.sleep(10);
            }
        }
    }

    private void stubStatusBarService(StatusBarServiceStub stub) {
        ServiceContainerUtil.replaceService(getProject(),
                RadicleStatusBarService.class, stub, this.getTestRootDisposable());
    }
}
