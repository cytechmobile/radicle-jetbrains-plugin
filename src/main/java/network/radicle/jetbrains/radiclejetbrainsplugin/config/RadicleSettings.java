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

public class RadicleSettings implements SearchableConfigurable {

    private static final Logger logger = Logger.getInstance(RadicleSettings.class);

    public static final String ID = RadicleBundle.message("radicle");
    public static final String browseFolderTitle = RadicleBundle.message("selectExecutable");

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;
    private JLabel radVersionLabel;
    private RadiclePath originalSavedPath;
    private final RadiclePathHandler radiclePathHandler;

    public RadicleSettings() {
        super();
        this.radiclePathHandler = new RadiclePathHandler();
        this.originalSavedPath = this.radiclePathHandler.loadPath();
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
            var version = RadicleBundle.message("radVersion");
            if (!Strings.isNullOrEmpty(radInfo)) {
                var radInfoParts = radInfo.split(" ");
                version += radInfoParts.length > 1 ? radInfoParts[1] : "";
            }
            updateRadVersionLabel(version);
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
        return Strings.isNullOrEmpty(selectedPath) || !selectedPath.equals(this.originalSavedPath.getPath());
    }

    @Override
    public void apply() throws ConfigurationException {
        String selectedPath = getSelectedPath();
        radiclePathHandler.savePath(selectedPath);
        this.originalSavedPath = this.radiclePathHandler.loadPath();
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
        radPathField.setText(this.originalSavedPath.getPath());
        radPathField.addBrowseFolderListener(browseFolderTitle, "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));
    }
}
