package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.cd;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.git;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.tac;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.append;
import static org.assertj.core.api.Assertions.assertThat;

public class GitTestUtil {
    private static final Logger logger = LoggerFactory.getLogger(GitTestUtil.class);

    @NotNull
    public static GitRepository createGitRepository(
            @NotNull Project project, @NotNull String remotePath) {
        try {
            cd(remotePath);
            git("init --initial-branch=main");
            setupGitConfig();
            tac("initial_file.txt");
            tac("initial_file_2.txt");
            VirtualFile gitDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(remotePath, GitUtil.DOT_GIT));
            assertThat(gitDir).isNotNull();
            return registerRepo(project, remotePath);
        } catch (Exception e) {
            logger.warn("Unable to create git repo");
            return null;
        }
    }

    public static void addRadRemote(Project project, GitRepository repo) {
        repo.getRemotes().add(new GitRemote("rad", List.of("rad://abcdef"), List.of("rad://abcdef"), List.of("rad://abcdef"), List.of("rad://abcdef")));
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) throws InterruptedException {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepositoryManager grm = GitUtil.getRepositoryManager(project);

        AtomicReference<GitRepository> repository = new AtomicReference<>();
        var countDown = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            repository.set(grm.getRepositoryForRoot(file));
            countDown.countDown();
        });
        countDown.await();
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        addRadRemote(project, repository.get());
        return repository.get();
    }

   public static void writeToFile(@NotNull File file, @NotNull String content) {
       try {
           append(file, content);
       } catch (Exception e) {
           logger.warn("Unable to write to the file");
       }
   }

    public static void setupGitConfig() {
        git("config user.name 'tester'");
        git("config user.email 'tester@cytech.gr'");
        git("config push.default simple");
    }

}
