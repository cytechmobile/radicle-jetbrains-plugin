package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.AsyncProcessIcon;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleGlobalSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.util.List;

public class AddSeedNodeDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField nodeField;
    private final List<SeedNode> loadedSeedNodes;
    private JTextField portField;
    private JLabel errorField;
    private JLabel progressLabel;
    private AsyncProcessIcon searchSpinner;

    public AddSeedNodeDialog(List<SeedNode> seedNodes) {
        super(true);
        portField.setText(RadicleGlobalSettingsHandler.DEFAULT_SEED_PORT);
        this.loadedSeedNodes = seedNodes;
        setTitle(RadicleBundle.message("addSeedNode"));
        init();
    }

    public AddSeedNodeDialog(String name, String port, List<SeedNode> seedNodes) {
        super(true);
        nodeField.setText(name);
        portField.setText(port);
        this.loadedSeedNodes = seedNodes;
        setTitle(RadicleBundle.message("editSeedNode"));
        init();
    }

    private boolean nodeExists() {
        var seedNode = new SeedNode(this.nodeField.getText(), this.portField.getText());
        if (loadedSeedNodes.contains(seedNode)) {
            return true;
        }
        return false;
    }

    private boolean isValidIp(String ip) {
        String pattern = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        return ip.matches(pattern);
    }

    private boolean isValidDomainName(String name) {
        String regex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
        return name.matches(regex);
    }

    private boolean isValidPort(String port) {
        String regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
        return port.matches(regex);
    }

    private boolean isApiReachable() {
        var projectApi = new ProjectApi();
        var seedNode = new SeedNode(nodeField.getText(), portField.getText());
        var radProjects = projectApi.fetchRadProjects(seedNode, 0);
        return radProjects != null;
    }

    private boolean isFormValid() {
        if (!(isValidIp(nodeField.getText()) || isValidDomainName(nodeField.getText()))) {
            this.errorField.setText(RadicleBundle.message("invalidSeedNode"));
            return false;
        } else if (!isValidPort(portField.getText())) {
            this.errorField.setText(RadicleBundle.message("invalidPort"));
            return false;
        } else if (nodeExists()) {
            this.errorField.setText(RadicleBundle.message("seedNodeExists"));
            return false;
        }
        return true;
    }

    @Override
    protected void doOKAction() {
        this.errorField.setText("");
        var isValid = isFormValid();
        if (!isValid) {
            return;
        }
        setOKActionEnabled(false);
        progressLabel.setVisible(true);
        searchSpinner.setVisible(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var isReachable = isApiReachable();
            ApplicationManager.getApplication().invokeLater(() -> {
                progressLabel.setVisible(false);
                searchSpinner.setVisible(false);
                setOKActionEnabled(true);
                if (!isReachable) {
                    this.errorField.setText(RadicleBundle.message("notReachable"));
                } else {
                    super.doOKAction();
                }
            }, ModalityState.any());
        });
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void init() {
        super.init();
        setOKActionEnabled(false);
        errorField.setForeground(JBColor.RED);
        progressLabel.setVisible(false);
        searchSpinner.setVisible(false);
        var textFieldListener = new TextFieldListener();
        nodeField.getDocument().addDocumentListener(textFieldListener);
        portField.getDocument().addDocumentListener(textFieldListener);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        var borderPanel = new JPanel(new BorderLayout());
        progressLabel = new JLabel(RadicleBundle.message("reachableMsg"));
        searchSpinner = new AsyncProcessIcon(RadicleBundle.message("reachableMsg"));
        borderPanel.add(contentPane, BorderLayout.CENTER);
        var myPanel = new JPanel();
        myPanel.add(progressLabel);
        myPanel.add(searchSpinner);
        borderPanel.add(myPanel, BorderLayout.SOUTH);
        return borderPanel;
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            setOKActionEnabled(!nodeField.getText().isEmpty() && !portField.getText().isEmpty());
        }
    }

    public JTextField getNodeField() {
        return nodeField;
    }

    public JTextField getPortField() {
        return portField;
    }

    public JLabel getErrorField() {
        return errorField;
    }
}
