package network.radicle.jetbrains.radiclejetbrainsplugin.dialog;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PushDialog extends VcsPushDialog {

    public PushDialog(@NotNull Project project, @NotNull List<? extends Repository> selectedRepositories, @Nullable Repository currentRepo) {
        super(project, selectedRepositories, currentRepo);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        var rps = super.myProject.getService(RadicleProjectService.class);
        rps.forceRadPush = false;
    }
}
