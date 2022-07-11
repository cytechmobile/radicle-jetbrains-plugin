package network.radicle.jetbrains.radiclejetbrainsplugin.config;

import com.google.common.base.Strings;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import network.radicle.jetbrains.radiclejetbrainsplugin.Util;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RadicleSettings implements SearchableConfigurable {
    public static final String ID = "Radicle";
    public static final String browseFolderTitle = "Select Executable";

    protected TextFieldWithBrowseButton radPathField;
    protected JPanel mainPanel;
    private JButton testButton;

    private RadiclePath originalSavedPath;
    private RadiclePathHandler radiclePathHandler;

    public RadicleSettings() {
        super();
        this.radiclePathHandler = new RadiclePathHandler();
        this.originalSavedPath = this.radiclePathHandler.loadPath();
        initFileChooserComponent();
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(Util.getOS());
            }
        });
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
    }

    private String getSelectedPath() {
        return radPathField.getText();
    }

    private void initFileChooserComponent() {
        radPathField.setText(this.originalSavedPath.getPath());
        radPathField.addBrowseFolderListener(browseFolderTitle, "", null,
                new FileChooserDescriptor(true, false, false, false, false, false));
    }
}
