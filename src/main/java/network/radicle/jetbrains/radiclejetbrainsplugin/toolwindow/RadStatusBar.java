package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsView;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleStatusBarService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getHorizontalPanel;
import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.getVerticalPanel;

public class RadStatusBar implements StatusBarWidgetFactory {
    public static final String ID = "RadStatusBar";

    public RadStatusBar() {
    }

    @Override
    public @NotNull @NonNls String getId() {
        return ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return RadicleBundle.message("radicle");
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project, @NotNull CoroutineScope scope) {
        return new RadWidget(project);
    }

    public static final class RadWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
        private final Project project;

        public RadWidget(Project project) {
            this.project = project;
        }

        @NotNull
        @Override
        public String ID() {
            return ID;
        }

        @Override
        public void install(@NotNull StatusBar statusBar) {
            StatusBarWidget.super.install(statusBar);
        }

        @Override
        public @NotNull WidgetPresentation getPresentation() {
            return this;
        }

        @Override
        public @NotNull Consumer<MouseEvent> getClickConsumer() {
            return this::showPopup;
        }

        private void showPopup(MouseEvent event) {
            var widget = event.getComponent();
            if (widget != null) {
                var statusPanel = createPanel();
                var builder = JBPopupFactory.getInstance().createComponentPopupBuilder(statusPanel, null);
                var popup = builder.createPopup();
                popup.show(new RelativePoint(widget, new Point(0, -statusPanel.getPreferredSize().height)));
            }
        }

        public JPanel createPanel() {
            return new StatusPanel(project);
        }

        @Override
        public @NotNull String getTooltipText() {
            return RadicleBundle.message("status.bar.tooltip");
        }

        @Override
        public @NotNull Icon getIcon() {
            Icon icon;
            var radicleSettingsHandler = new RadicleProjectSettingsHandler(project);
            var statusBarService = project.getService(RadicleStatusBarService.class);
            if (radicleSettingsHandler.isSettingsEmpty()) {
                icon = RadicleIcons.RADICLE_STATUS_BAR_MISSING_SETTINGS;
            } else if (!(statusBarService.isNodeRunning() && statusBarService.isHttpdRunning())) {
                icon = RadicleIcons.RADICLE_STATUS_BAR_SERVICES_NOT_RUNNING;
            } else {
                icon = RadicleIcons.RADICLE_STATUS_BAR_SERVICES_RUNNING;
            }
            return icon;
        }

        public static class StatusPanel extends JPanel {
            private final Project project;
            private final RadicleProjectSettingsHandler settingsHandler;

            public StatusPanel(Project project) {
                this.project = project;
                this.settingsHandler = new RadicleProjectSettingsHandler(project);
                this.setLayout(new BorderLayout());
                this.setBorder(new EmptyBorder(10, 10, 10, 10));
                this.init();
            }

            private void init() {
                var configure = new JButton(RadicleBundle.message("configure"));
                configure.addActionListener(e ->
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, RadicleSettingsView.class));

                if (settingsHandler.isSettingsEmpty()) {
                    var titlePanel = new JPanel();
                    titlePanel.add(new JLabel(RadicleBundle.message("missing.settings"), SwingConstants.CENTER));
                    titlePanel.setBorder(new EmptyBorder(0, 0, 5, 0));
                    add(titlePanel, BorderLayout.NORTH);
                    add(configure, BorderLayout.CENTER);
                    return;
                }

                var statusBarService = project.getService(RadicleStatusBarService.class);

                var nodeRunningLabel = new JLabel(RadicleBundle.message("radicle.node"));
                nodeRunningLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                nodeRunningLabel.setIconTextGap(2);
                var nodeIcon = statusBarService.isNodeRunning() ? AllIcons.RunConfigurations.TestPassed : AllIcons.RunConfigurations.TestError;
                nodeRunningLabel.setIcon(nodeIcon);

                var httpdRunningLabel = new JLabel(RadicleBundle.message("radicle.httpd"));
                httpdRunningLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                httpdRunningLabel.setIconTextGap(2);
                var httpdIcon = statusBarService.isHttpdRunning() ? AllIcons.RunConfigurations.TestPassed : AllIcons.RunConfigurations.TestError;
                httpdRunningLabel.setIcon(httpdIcon);

                var nodeStatusPanel = getHorizontalPanel(2);
                nodeStatusPanel.add(nodeRunningLabel);

                var httpdStatusPanel = getHorizontalPanel(2);
                httpdStatusPanel.add(httpdRunningLabel);

                var verticalPanel = getVerticalPanel(2);
                verticalPanel.add(nodeStatusPanel);
                verticalPanel.add(httpdStatusPanel);

                var titlePanel = new JPanel();
                titlePanel.add(new JLabel(RadicleBundle.message("status"), SwingConstants.CENTER));
                titlePanel.setBorder(new EmptyBorder(0, 0, 5, 0));
                add(titlePanel, BorderLayout.NORTH);
                add(verticalPanel, BorderLayout.CENTER);
            }
        }
    }
}
