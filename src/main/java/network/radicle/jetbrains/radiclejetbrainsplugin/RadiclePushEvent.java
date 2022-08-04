package network.radicle.jetbrains.radiclejetbrainsplugin;

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

public class RadiclePushEvent extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (!BasicAction.isCliPathConfigured(e) || !BasicAction.hasGitRepos(e)) {
            return ;
        }

        openGitPushDialog(project, e);
        var rps = project.getService(RadicleProjectService.class);
        rps.forceRadPush = true;
    }

    private void openGitPushDialog(Project project , @NotNull AnActionEvent e) {
        VcsRepositoryManager manager = VcsRepositoryManager.getInstance(project);
        Collection<Repository> repositories = e.getData(CommonDataKeys.EDITOR) != null
                ? ContainerUtil.emptyList()
                : collectRepositories(manager, e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));
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
