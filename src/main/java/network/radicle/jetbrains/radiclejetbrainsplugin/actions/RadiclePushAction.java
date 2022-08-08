package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.BasicAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class RadiclePushAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        performAction(project, e);
    }

    public void performAction(Project project, @Nullable AnActionEvent e) {
        if (!BasicAction.isCliPathConfigured(project) || !BasicAction.hasGitRepos(project) ||
                !BasicAction.isSeedNodeConfigured(project) || !BasicAction.isRadInitialized(project)) {
            return ;
        }

        var rps = project.getService(RadicleProjectService.class);
        // TODO check what happens if git push dialog is canceled
        rps.forceRadPush = true;
        openGitPushDialog(project, e);
    }

    private void openGitPushDialog(@NotNull Project project , @Nullable AnActionEvent e) {
        VcsRepositoryManager manager = VcsRepositoryManager.getInstance(project);
        final Collection<Repository> repositories = e == null || e.getData(CommonDataKeys.EDITOR) != null ?
                ContainerUtil.emptyList() :
                collectRepositories(manager, e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));
        VirtualFile selectedFile = DvcsUtil.getSelectedFile(project);
        new VcsPushDialog(project, DvcsUtil.sortRepositories(repositories),
                selectedFile != null ? manager.getRepositoryForFileQuick(selectedFile) : null).show();
    }

    @NotNull
    private static Collection<Repository> collectRepositories(@NotNull VcsRepositoryManager vcsRepositoryManager,
                                                              VirtualFile @Nullable [] files) {
        if (files == null) return Collections.emptyList();
        Collection<Repository> repositories = new HashSet<>();
        for (VirtualFile file : files) {
            Repository repo = vcsRepositoryManager.getRepositoryForFileQuick(file);
            if (repo != null) {
                repositories.add(repo);
            }
        }
        return repositories;
    }

}
