package network.radicle.jetbrains.radiclejetbrainsplugin.dialog.clone;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.icons.RadicleIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public class CloneRadDialogExtension implements VcsCloneDialogExtension {

    @NotNull
    @Override
    public VcsCloneDialogExtensionComponent createMainComponent(@NotNull Project project) {
        return new CloneRadDialog(project);
    }

    @NotNull
    @Override
    public VcsCloneDialogExtensionComponent createMainComponent(@NotNull Project project, @NotNull ModalityState modalityState) {
        return createMainComponent(project);
    }

    @NotNull
    @Override
    public List<VcsCloneDialogExtensionStatusLine> getAdditionalStatusLines() {
        return List.of();
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RadicleIcons.RADICLE;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return RadicleBundle.message("radicle");
    }

    @Nls
    @Nullable
    @Override
    public String getTooltip() {
        return null;
    }

}
