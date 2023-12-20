package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.Action;
import javax.swing.event.DocumentEvent;

public class IdentityDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField passphraseField;
    private JTextField aliasField;
    private JLabel aliasLabel;
    private boolean hasIdentity;

    public IdentityDialog() {
        super(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    public boolean showAndGet() {
        if (hasIdentity) {
            aliasField.setVisible(false);
            aliasLabel.setVisible(false);
        }
        var isFormValid =  !aliasField.getText().isEmpty() || !aliasField.isVisible();
        setOKActionEnabled(isFormValid);
        return super.showAndGet();
    }

    public void hasIdentity(boolean identity) {
        this.hasIdentity = identity;
    }

    public String getPassword() {
        return passphraseField.getText();
    }

    public String getAlias() {
        return aliasField.getText();
    }

    public void setAlias(String alias) {
        aliasField.setText(alias);
    }

    public void setPassphrase(String passphrase) {
        passphraseField.setText(passphrase);
    }

    protected void init() {
        super.init();
        var textFieldListener = new TextFieldListener();
        aliasField.getDocument().addDocumentListener(textFieldListener);
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            setOKActionEnabled(!aliasField.getText().isEmpty() || !aliasField.isVisible());
        }
    }
}
