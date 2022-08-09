package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettings;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectActionDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel allowRadLabel;
    private JRadioButton yesRadio;
    private JRadioButton noRadio;
    private JCheckBox remember;
    private List<GitRepository> repos;
    private final RadicleSettingsHandler radicleSettingsHandler;
    private final Project project;

    public SelectActionDialog(@NotNull Project project, List<GitRepository> repos) {
        super(true);
        this.radicleSettingsHandler = new RadicleSettingsHandler();
        this.repos = repos;
        this.project = project;
        init();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if (yesRadio.isSelected()) {
            var syncEvent = new RadicleSyncAction();
            syncEvent.performAction(project, repos);
        }

        if (remember.isSelected()) {
            var radType = yesRadio.isSelected() ? RadicleSettings.RadSyncType.YES :
                    RadicleSettings.RadSyncType.NO;
            radicleSettingsHandler.saveRadSync(radType);
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] {remember.getAction(),getOKAction(),getCancelAction()};
    }

    @Override
    protected void init() {
        super.init();
        yesRadio.addActionListener(e -> {
            noRadio.setSelected(false);
            setOKActionEnabled(true);
        });
        noRadio.addActionListener(e -> {
            yesRadio.setSelected(false);
            setOKActionEnabled(true);
        });
        allowRadLabel.setText(RadicleBundle.message("allowRad"));
        setTitle(RadicleBundle.message("radDetected"));
        setOKActionEnabled(!noRadio.isEnabled() && !yesRadio.isEnabled());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    public JCheckBox getRememberCheckBox() {
        return remember;
    }

    public JRadioButton getYesRadio() {
        return yesRadio;
    }

    public JRadioButton getNoRadio() {
        return noRadio;
    }
}
