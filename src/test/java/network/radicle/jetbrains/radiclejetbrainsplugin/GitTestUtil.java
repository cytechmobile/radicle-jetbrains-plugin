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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GitTestUtil {
    private static final Logger logger = LoggerFactory.getLogger(GitTestUtil.class);

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
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepositoryManager grm = GitUtil.getRepositoryManager(project);

        GitRepository repository = grm.getRepositoryForRoot(file);
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        return repository;
    }

    public static void writeToFile(@NotNull File file, @NotNull String content) {
        try {
            append(file,content);
        } catch (Exception e) {
            logger.warn("Unable to write to the file");
        }
    }

    public static void addAll(File workingDir) {
        run(workingDir, List.of("git.exe","add","--verbose","."),false);
    }

    public static void commit(File workingDir, String comment) {
        run(workingDir, List.of("git.exe","commit","-m",comment),false);
    }

    public static void setupGitConfig() {
        git("config user.name 'stelios'");
        git("config user.email 'steliosmavr@cytech.gr'");
        git("config push.default simple");
    }

}
