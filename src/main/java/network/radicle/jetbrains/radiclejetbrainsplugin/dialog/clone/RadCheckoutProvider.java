package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.dvcs.DvcsRememberedInputs;
import com.intellij.dvcs.ui.DvcsCloneDialogComponent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RadCheckoutProvider implements CheckoutProvider {

    @Override
    public @NotNull VcsCloneComponent buildVcsCloneComponent(@NotNull Project project, @NotNull ModalityState modalityState,
                                                             @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
        return new VcsCloneComponentExt(project, "", new DvcsRememberedInputs(), dialogStateListener);
    }

    @Override
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {

    }

    @Override
    public @NonNls String getVcsName() {
        return RadicleBundle.message("radicle");
    }

    public static class VcsCloneComponentExt extends  DvcsCloneDialogComponent {
        private final Project project;
        public VcsCloneComponentExt(@NotNull Project project, @NotNull String vcsDirectoryName, @NotNull DvcsRememberedInputs rememberedInputs,
                                    @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
            super(project, vcsDirectoryName, rememberedInputs, dialogStateListener);
            this.project = project;
        }

        @Override
        public void doClone(@NotNull CheckoutProvider.Listener listener) {
            if (!BasicAction.isCliPathConfigured(project)) {
                return ;
            }
            CloneUtil.doClone(listener,project, getUrl(),null, getDirectory());
        }
    }
}
