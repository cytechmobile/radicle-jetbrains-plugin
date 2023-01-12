package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.JBColor;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

public class RadicleSettingsView implements SearchableConfigurable {
    private static final Logger logger = Logger.getInstance(RadicleSettingsView.class);
    private static final String SUPPORTED_CLI_VERSION = "0.6.1";

    public static final String ID = RadicleBundle.message("radicle");

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;
    private JLabel radVersionLabel;
    private JComboBox radSyncList;
    private JLabel enforceVersionLabel;
    private RadicleSettings settings;
    private final RadicleSettingsHandler radicleSettingsHandler;

    public RadicleSettingsView() {
        super();
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.settings = this.radicleSettingsHandler.loadSettings();
        initComponents();
    }

    protected String getRadVersion() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        if (Strings.isNullOrEmpty(getSelectedPath())) {
            return "";
        }
        ProcessOutput output = rad.getVersion(getSelectedPath());
        var radInfo = output.getStdout();
        if (!Strings.isNullOrEmpty(radInfo) && radInfo.contains("rad")) {
            var radInfoParts = radInfo.split(" ");
            return radInfoParts.length > 1 ? radInfoParts[1] : "";
        }
        return "";
    }

    protected String getRadPath() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        ProcessOutput output = rad.getRadPath();
        var pathInfo = output.getStdoutLines();
        /* which command return empty and where command return INFO if the os cant find the program path */
        if (pathInfo.size() > 0 && !Strings.isNullOrEmpty(pathInfo.get(0)) && !pathInfo.get(0).contains("INFO")) {
            return pathInfo.get(0);
        }
        return "";
    }

    private void updateRadVersionLabel() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var msg = RadicleBundle.message("radNotInstalled");
            var version = getRadVersion();
            if (!Strings.isNullOrEmpty(version)) {
                msg = RadicleBundle.message("radVersion") + " " + version;
            }
            String finalMsg = msg;
            ApplicationManager.getApplication().invokeLater(() -> {
                radVersionLabel.setText(finalMsg);
                showHideEnforceLabel(version, false);
            }, ModalityState.any());
        });
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return ID;
    }

    @Override
    @Nullable
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        String selectedPath = getSelectedPath();
        var radSync = (ComboItem) radSyncList.getSelectedItem();
        return !selectedPath.equals(this.settings.getPath()) ||
                !radSync.key.equals(this.settings.getRadSync());
    }


    @Override
    public void apply() {
        var path = getSelectedPath();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radSync = (ComboItem) radSyncList.getSelectedItem();
            radicleSettingsHandler.saveRadSync(RadicleSettings.RadSyncType.from(radSync.key));
            var version = getRadVersion();
            var isPathValid = !Strings.isNullOrEmpty(version);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (isPathValid) {
                    radicleSettingsHandler.savePath(path);
                }
                settings = this.radicleSettingsHandler.loadSettings();
                showHideEnforceLabel(version, true);
            }, ModalityState.any());
        });
    }

    private void initListeners() {
        testButton.addActionListener(e ->
                ApplicationManager.getApplication().executeOnPooledThread(this::updateRadVersionLabel));

        radPathField.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (radPathField.getText().contains(RadicleBundle.message("autoDetected"))) {
                    radPathField.setText("");
                    radPathField.getTextField().setForeground(JBColor.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getSelectedPath().isEmpty()) {
                    updateTextFieldPlaceholder();
                }
            }
        });
    }

    protected void updateTextFieldPlaceholder() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String path = getRadPath();
            if (!Strings.isNullOrEmpty(path) && radPathField != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    radPathField.getTextField().setForeground(JBColor.GRAY);
                    radPathField.setText(RadicleBundle.message("autoDetected") + path);
                }, ModalityState.any());
            }
        });
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        radPathField = null;
        testButton = null;
        radVersionLabel = null;
    }

    private String getSelectedPath() {
        var path = radPathField != null ? radPathField.getText() : "";
        if (path.contains(RadicleBundle.message("autoDetected"))) {
            path = path.split(":")[1].trim();
        }
        return path;
    }

    private void initComponents() {
        radPathField.setText(this.settings.getPath());
        radPathField.addBrowseFolderListener(RadicleBundle.message("selectExecutable"), "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));

        var askItem = new ComboItem(RadicleSettings.RadSyncType.ASK.val, RadicleSettings.RadSyncType.ASK.name);
        var yesItem = new ComboItem(RadicleSettings.RadSyncType.YES.val, RadicleSettings.RadSyncType.YES.name);
        var noItem = new ComboItem(RadicleSettings.RadSyncType.NO.val, RadicleSettings.RadSyncType.NO.name);

        enforceVersionLabel.setVisible(false);
        enforceVersionLabel.setText(RadicleBundle.message("unSupportedCliVer"));
        enforceVersionLabel.setFont(enforceVersionLabel.getFont().deriveFont(Font.BOLD));

        radSyncList.addItem(askItem);
        radSyncList.addItem(yesItem);
        radSyncList.addItem(noItem);

        var radSync = this.settings.getRadSync();
        radSyncList.setSelectedIndex(radSync);
        initListeners();
        if (Strings.isNullOrEmpty(getSelectedPath())) {
            updateTextFieldPlaceholder();
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var version = getRadVersion();
            ApplicationManager.getApplication().invokeLater(() -> {
                showHideEnforceLabel(version, false);
            }, ModalityState.any());
        });
    }

    private void showHideEnforceLabel(String radVersion, boolean isApply) {
        if (Strings.isNullOrEmpty(radVersion)) {
            return;
        }
        enforceVersionLabel.setVisible(false);
        var isCompatibleVersion = radVersion.replace("\n", "").trim().equals(SUPPORTED_CLI_VERSION);
        if (!isCompatibleVersion) {
            enforceVersionLabel.setVisible(true);
            if (isApply) {
                RadAction.showNotification(null, "radCliWarning", "unSupportedCliVer", NotificationType.WARNING,
                        List.of(new RadAction.ConfigureRadCliNotificationAction(null, RadicleBundle.lazyMessage("configure"))));
            }
        }
    }

    public JComboBox getComboBox() {
        return radSyncList;
    }

    public JLabel getRadVersionLabel() {
        return radVersionLabel;
    }

    public JButton getTestButton() {
        return testButton;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public TextFieldWithBrowseButton getPathField() {
        return radPathField;
    }

    private class ComboItem {
        private Integer key;
        private String value;

        public ComboItem(Integer key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

}
