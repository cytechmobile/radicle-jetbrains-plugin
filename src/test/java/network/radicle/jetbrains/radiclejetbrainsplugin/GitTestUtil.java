package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GitTestUtil {

    @NotNull
    public static GitRepository createGitRepository(
            @NotNull Project project, @NotNull String remotePath) {
        cd(remotePath);
        git("init");
        setupGitConfig();
        tac("initial_file.txt");
        VirtualFile gitDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(remotePath, GitUtil.DOT_GIT));
        assertThat(gitDir).isNotNull();
        return registerRepo(project, remotePath);
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepositoryManager grm = GitUtil.getRepositoryManager(project);

        GitRepository repository = grm.getRepositoryForRoot(file);
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        return repository;
    }

    public static void setupGitConfig() {
        git("config user.name 'stelios'");
        git("config user.email 'steliosmavr@cytech.gr'");
        git("config push.default simple");
    }

}
