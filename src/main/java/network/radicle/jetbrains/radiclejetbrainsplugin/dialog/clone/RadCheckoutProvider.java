package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.dvcs.DvcsRememberedInputs;
import com.intellij.dvcs.ui.DvcsCloneDialogComponent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.VcsCloneComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.util.containers.ContainerUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.util.List;

public class RadCheckoutProvider implements CheckoutProvider {

    @Override
    public @NotNull VcsCloneComponent buildVcsCloneComponent(@NotNull Project project, @NotNull ModalityState modalityState,
                                                             @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
        return new VcsCloneComponentExt(project, "", new DvcsRememberedInputs(), dialogStateListener);
    }

    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {

    }

    @Override
    public @NonNls String getVcsName() {
        return RadicleBundle.message("radicle");
    }

    public static class VcsCloneComponentExt extends DvcsCloneDialogComponent implements CloneProject {

        private final Project project;
        private TextFieldWithHistory urlField;

        public VcsCloneComponentExt(@NotNull Project project, @NotNull String vcsDirectoryName, @NotNull DvcsRememberedInputs rememberedInputs,
                                    @NotNull VcsCloneDialogComponentStateListener dialogStateListener) {
            super(project, vcsDirectoryName, rememberedInputs, dialogStateListener);
            this.project = project;
            getFields();
        }

        private void getFields() {
            var mainPanel = getMainPanel();
            var components = mainPanel.getComponents();
            for (Component component : components) {
                if (component.getClass().equals(TextFieldWithHistory.class)) {
                    urlField = (TextFieldWithHistory) component;
                }
            }
        }

        @NotNull
        @Override
        public List<ValidationInfo> doValidateAll() {
            var list = super.doValidateAll();
            if (urlField != null && !isValidUrl(urlField.getText())) {
                ContainerUtil.addIfNotNull(list, new ValidationInfo(RadicleBundle.message("invalidUrl"), urlField));
            }
            return list;
        }

        private boolean isValidUrl(String url) {
            return url.startsWith("rad://");
        }

        /* This doClone is for 2022 version */
        public void doClone(@NotNull CheckoutProvider.Listener listener) {
            if (!RadAction.isCliPathConfigured(project)) {
                return;
            }
            CloneUtil.doClone(listener, project, this);
        }

        /* This doClone is for 2020 / 2021 versions */
        public void doClone(@NotNull Project pr, @NotNull CheckoutProvider.Listener listener) {
            if (!RadAction.isCliPathConfigured(project)) {
                return;
            }
            CloneUtil.doClone(listener, project, this);
        }

        @Override
        public String url() {
            return getUrl();
        }

        @Override
        public String directory() {
            return getDirectory();
        }
    }
}
