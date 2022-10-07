package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ui.cloneDialog.VcsCloneDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone.RadCheckoutProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RadicleCloneAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VcsCloneDialog dialog = new VcsCloneDialog.Builder(Objects.requireNonNull(e.getProject())).forVcs(RadCheckoutProvider.class);
        dialog.showAndGet();
    }
}
