package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ConfirmationDialog extends DialogWrapper {

    private JPanel contentPane;
    private JLabel msgLabel;

    public ConfirmationDialog(String msg, String title) {
        super(true);
        this.msgLabel.setText(msg);
        setTitle(title);
        init();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }


}
