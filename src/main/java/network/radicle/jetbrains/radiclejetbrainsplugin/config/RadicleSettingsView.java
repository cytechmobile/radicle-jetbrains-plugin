package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleApplicationService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RadicleSettingsView implements SearchableConfigurable {

    private static final Logger logger = Logger.getInstance(RadicleSettingsView.class);

    public static final String ID = RadicleBundle.message("radicle");
    public static final String browseFolderTitle = RadicleBundle.message("selectExecutable");

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;
    private JLabel radVersionLabel;
    private JCheckBox runRadSync;
    private RadicleSettings settings;
    private final RadicleSettingsHandler radicleSettingsHandler;

    public RadicleSettingsView() {
        super();
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.settings = this.radicleSettingsHandler.loadSettings();
        initComponents();
        testButton.addActionListener(e ->
                ApplicationManager.getApplication().executeOnPooledThread(this::getRadVersion));
    }

    private void getRadVersion() {
        var rad = ApplicationManager.getApplication().getService(RadicleApplicationService.class);
        ProcessOutput output = rad.getVersion();
        var radInfo = output.getStdout();
        var msg = RadicleBundle.message("radNotInstalled");
        if (!Strings.isNullOrEmpty(radInfo) && radInfo.contains("rad")) {
            var radInfoParts = radInfo.split(" ");
            var version = radInfoParts.length > 1 ? radInfoParts[1] : "";
            msg = RadicleBundle.message("radVersion") + version;
        }
        updateRadVersionLabel(msg);
    }

    private void updateRadVersionLabel(String msg) {
        ApplicationManager.getApplication().invokeLater(() ->
                radVersionLabel.setText(msg), ModalityState.any());
    }

    @Override @NotNull @NonNls
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return ID;
    }

    @Override @Nullable
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        String selectedPath = getSelectedPath();
        Boolean radSync = getRadSyncSelected();
        return Strings.isNullOrEmpty(selectedPath) || !selectedPath.equals(this.settings.getPath()) ||
                !radSync.equals(Boolean.parseBoolean(this.settings.getRadSync()));
    }

    @Override
    public void apply() throws ConfigurationException {
        radicleSettingsHandler.savePath(getSelectedPath());
        radicleSettingsHandler.saveRadSync(getRadSyncSelected().toString());
        settings = this.radicleSettingsHandler.loadSettings();
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        radPathField = null;
        testButton = null;
        radVersionLabel = null;
    }

    private String getSelectedPath() {
        return radPathField.getText();
    }

    private Boolean getRadSyncSelected() {
        return runRadSync.isSelected();
    }

    private void initComponents() {
        radPathField.setText(this.settings.getPath());
        radPathField.addBrowseFolderListener(browseFolderTitle, "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));
        var radSync = this.settings.getRadSync();
        runRadSync.setSelected(!Strings.isNullOrEmpty(radSync) && Boolean.parseBoolean(radSync));
    }
}
