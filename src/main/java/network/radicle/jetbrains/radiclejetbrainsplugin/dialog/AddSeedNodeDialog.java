package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.List;

public class AddSeedNodeDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField nodeField;
    private List<String> loadedSeedNodes;
    private JTextField portField;
    private JLabel errorField;

    public AddSeedNodeDialog(List<String> seedNodes) {
        super(true);
        portField.setText(RadicleSettingsHandler.DEFAULT_SEED_PORT);
        this.loadedSeedNodes = seedNodes;
        setTitle(RadicleBundle.message("addSeedNode"));
        init();
    }

    public AddSeedNodeDialog(String name, String port, List<String> seedNodes) {
        super(true);
        nodeField.setText(name);
        portField.setText(port);
        this.loadedSeedNodes = seedNodes;
        setTitle(RadicleBundle.message("editSeedNode"));
        init();
    }

    private boolean isNodeExists() {
        var seedNode = this.nodeField.getText() + RadicleSettingsHandler.RAD_SEED_SEPERATOR + this.portField.getText();
        if (loadedSeedNodes.contains(seedNode)){
            return true;
        }
        return false;
    }

    private boolean isValidIp(String ip) {
        String pattern = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        return ip.matches(pattern);
    }

    private boolean isValidDomainName(String name) {
        String regex = "^((?!-)[A-Za-z0-9-]"
                + "{1,63}(?<!-)\\.)"
                + "+[A-Za-z]{2,6}";
        return name.matches(regex);
    }

    private boolean isValidPort(String port) {
        String regex = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
        return port.matches(regex);
    }

    @Override
    protected void doOKAction() {
        if (!(isValidIp(nodeField.getText()) || isValidDomainName(nodeField.getText()))) {
            this.errorField.setText(RadicleBundle.message("invalidSeedNode"));
        } else if (!isValidPort(portField.getText())){
            this.errorField.setText(RadicleBundle.message("invalidPort"));
        } else if(isNodeExists()) {
            this.errorField.setText(RadicleBundle.message("seedNodeExists"));
        } else {
            super.doOKAction();
        }
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
        var textFieldListener = new TextFieldListener();
        nodeField.getDocument().addDocumentListener(textFieldListener);
        portField.getDocument().addDocumentListener(textFieldListener);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            if (nodeField.getText().isEmpty() || portField.getText().isEmpty()) {
                setOKActionEnabled(false);
            } else {
                setOKActionEnabled(true);
            }
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
