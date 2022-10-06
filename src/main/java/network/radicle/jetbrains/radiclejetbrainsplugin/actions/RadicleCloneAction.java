package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ui.cloneDialog.VcsCloneDialog;
import git4idea.checkout.GitCheckoutProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RadicleCloneAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VcsCloneDialog dialog = new VcsCloneDialog.Builder(Objects.requireNonNull(e.getProject())).forVcs(GitCheckoutProvider.class);
        dialog.showAndGet();
    }
}