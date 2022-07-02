package com.github.jchrist.radicleintellijplugin.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RadicleSettings implements SearchableConfigurable {
    public static final String ID = "Radicle";

    protected JTextField radPathField;
    protected JPanel mainPanel;

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
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        radPathField = null;
    }
}
