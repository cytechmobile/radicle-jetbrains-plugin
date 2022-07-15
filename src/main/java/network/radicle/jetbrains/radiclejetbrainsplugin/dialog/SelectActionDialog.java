package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.dvcs.push.PushInfo;
import com.intellij.openapi.ui.DialogWrapper;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectActionDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel allowRadLabel;
    private List<PushInfo> pushInfoList;


    public SelectActionDialog(List<PushInfo> pushInfoList) {
        super(true);
        this.pushInfoList = pushInfoList;
        init();
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
        return new Action[] {getOKAction(),getCancelAction()};
    }

    @Override
    protected void init() {
        super.init();
        setTitle(RadicleBundle.message("radDetected"));
        allowRadLabel.setText(RadicleBundle.message("allowRad"));
        setOKButtonText(RadicleBundle.message("always"));
        setCancelButtonText(RadicleBundle.message("justOnce"));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
