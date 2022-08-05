package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.RadicleSyncAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectActionDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel allowRadLabel;
    private JRadioButton once;
    private JRadioButton rememberMe;
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
        if (rememberMe.isSelected()) {
            radicleSettingsHandler.saveRadSync(Boolean.toString(true));
        } else if (once.isSelected()) {
            radicleSettingsHandler.saveRadSync(Boolean.toString(false));
        }
        if (rememberMe.isSelected() || once.isSelected()) {
            var syncEvent = new RadicleSyncAction();
            syncEvent.performAction(project, repos);
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        radicleSettingsHandler.saveRadSync(Boolean.toString(false));
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] {getOKAction(),getCancelAction()};
    }

    @Override
    protected void init() {
        super.init();
        once.addActionListener(e -> {
            rememberMe.setSelected(false);
            setOKActionEnabled(true);
        });
        rememberMe.addActionListener(e -> {
            once.setSelected(false);
            setOKActionEnabled(true);
        });
        allowRadLabel.setText(RadicleBundle.message("allowRad"));
        setTitle(RadicleBundle.message("radDetected"));
        setOKActionEnabled(!rememberMe.isEnabled() && !once.isEnabled());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
