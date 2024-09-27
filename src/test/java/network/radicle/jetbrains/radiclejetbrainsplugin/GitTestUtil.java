package network.radicle.jetbrains.radiclejetbrainsplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitCommit;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.GitImpl;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.append;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.cd;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.git;
import static network.radicle.jetbrains.radiclejetbrainsplugin.GitExecutor.tac;
import static org.assertj.core.api.Assertions.assertThat;

public class GitTestUtil {
    private static final Logger logger = LoggerFactory.getLogger(GitTestUtil.class);

    public static GitRepository createGitRepository(@NotNull Project project, @NotNull String remotePath) throws Exception {
        cd(remotePath);
        git("init --initial-branch=main");
        setupGitConfig();
        tac("initial_file.txt");
        tac("initial_file_2.txt");
        VirtualFile gitDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(remotePath, GitUtil.DOT_GIT));
        assertThat(gitDir).isNotNull();
        return registerRepo(project, remotePath);
    }

    public static void addRadRemote(GitRepository repo) {
        AbstractIT.runInBackground(repo.getProject(), () -> {
            try {
                var gitImpl = new GitImpl();
                gitImpl.addRemote(repo, "rad", "rad://abcdef");
                repo.update();
            } catch (Exception e) {
                logger.warn("Unable to add remote", e);
            }
        });
    }

    public static GitCommit findCommit(GitRepository repo, String revNumber) throws VcsException {
        var commits = GitHistoryUtils.history(repo.getProject(), repo.getRoot());
        for (var commit : commits) {
            if (commit.getId().asString().equals(revNumber)) {
                return commit;
            }
        }
        return null;
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) throws Exception {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepositoryManager grm = GitUtil.getRepositoryManager(project);

        AtomicReference<GitRepository> repository = new AtomicReference<>();
        AbstractIT.runInBackground(project, () -> repository.set(grm.getRepositoryForRoot(file)));
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        addRadRemote(repository.get());
        AbstractIT.executeUiTasks(project);
        assertThat(repository.get().getCurrentBranchName()).isNotBlank();
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
