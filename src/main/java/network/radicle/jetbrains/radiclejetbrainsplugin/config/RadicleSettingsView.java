package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;

public class RadicleSettingsView implements SearchableConfigurable {

    private static final Logger logger = Logger.getInstance(RadicleSettingsView.class);

    public static final String ID = RadicleBundle.message("radicle");
    public static final String browseFolderTitle = RadicleBundle.message("selectExecutable");

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;
    private JLabel radVersionLabel;
    private RadicleSettings settings;
    private final RadicleSettingsHandler radicleSettingsHandler;

    public RadicleSettingsView() {
        super();
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.settings = this.radicleSettingsHandler.loadSettings();
        initComponents();
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    public void run() {
                        getRadVersion();
                    }
                });
            }
        });
    }

    private void getRadVersion() {
        GeneralCommandLine cmdLine = null;
        if (SystemInfo.isWindows) {
            //TODO remove wsl
            cmdLine = new GeneralCommandLine("wsl",radPathField.getText(),"--version");
        } else {
            cmdLine = new GeneralCommandLine(radPathField.getText(),"--version");
        }
        cmdLine.setCharset(StandardCharsets.UTF_8);
        try {
            ProcessOutput output = ExecUtil.execAndGetOutput(cmdLine);
            var radInfo = output.getStdout();
            var msg = RadicleBundle.message("radNotInstalled");
            if (!Strings.isNullOrEmpty(radInfo) && radInfo.contains("rad")) {
                var radInfoParts = radInfo.split(" ");
                var version = radInfoParts.length > 1 ? radInfoParts[1] : "";
                msg = RadicleBundle.message("radVersion") + version;
            }
            updateRadVersionLabel(msg);
        } catch (ExecutionException ex) {
            logger.error("unable to run rad executable",ex.getMessage());
            updateRadVersionLabel(ex.getMessage());
        }
    }

    private void updateRadVersionLabel(String msg) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                radVersionLabel.setText(msg);
            }
        }, ModalityState.any());
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
        return Strings.isNullOrEmpty(selectedPath) || !selectedPath.equals(this.settings.getPath());
    }

    @Override
    public void apply() throws ConfigurationException {
        String selectedPath = getSelectedPath();
        radicleSettingsHandler.savePath(selectedPath);
        this.settings = this.radicleSettingsHandler.loadSettings();
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

    private void initComponents() {
        radPathField.setText(this.settings.getPath());
        radPathField.addBrowseFolderListener(browseFolderTitle, "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));
    }
}
