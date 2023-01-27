package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.util.ui.cloneDialog.VcsCloneDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone.RadCheckoutProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RadicleCloneAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        RadAction.showRadIcon(e);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        CheckoutProvider.Listener checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        VcsCloneDialog dialog = new VcsCloneDialog.Builder(Objects.requireNonNull(e.getProject())).forVcs(RadCheckoutProvider.class);
        if (dialog.showAndGet()) {
            dialog.doClone(checkoutListener);
        }
    }
}

