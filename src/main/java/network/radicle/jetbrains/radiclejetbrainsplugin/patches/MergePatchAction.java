package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.ide.file.BatchFileChangeListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.autoimport.ProjectBatchFileChangeListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitBranch;
import git4idea.GitLocalBranch;
import git4idea.GitReference;
import git4idea.GitVcs;
import git4idea.actions.GitStash;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBranchWorker;
import git4idea.branch.GitBrancher;
import git4idea.commands.Git;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRemote;
import git4idea.stash.GitStashOperations;
import git4idea.stash.GitStashUtils;
import kotlin.jvm.functions.Function1;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.showErrorNotification;

public class MergePatchAction extends AbstractAction {
    private static final Logger logger = Logger.getInstance(MergePatchAction.class);

    public final JButton mergeBtn;
    public final SingleValueModel<RadPatch> patchModel;
    public final RadPatch patch;

    protected GitRemote patchRemote;
    protected String patchRemoteBranchName;
    protected boolean shouldCleanup;
    protected boolean shouldCheckoutBranch;
    protected boolean initialBranchTrackedPatch;
    protected GitBranch initialBranch;
    protected GitLocalBranch localDefaultBranch;
    protected boolean merged;
    protected Future<?> performed;

    public MergePatchAction(JButton mergeBtn, SingleValueModel<RadPatch> patchModel) {
        this.mergeBtn = mergeBtn;
        this.patchModel = patchModel;
        this.patch = patchModel.getValue();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        mergeBtn.setEnabled(false);
        merged = false;
        performed = ApplicationManager.getApplication().executeOnPooledThread(this::mergePatch);
    }

    protected void mergePatch() {
        patchRemote = patch.repo.getRemotes().stream().filter(r -> Strings.nullToEmpty(r.getFirstUrl()).startsWith("rad://")).findFirst().orElse(null);
        if (patchRemote == null) {
            showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"), RadicleBundle.message("mergePatchErrorNoRemote"));
            return;
        }
        patchRemoteBranchName = patchRemote.getName() + "/patches/" + patch.id;
        shouldCleanup = false;
        shouldCheckoutBranch = false;
        initialBranchTrackedPatch = false;
        initialBranch = null;
        try {
            localDefaultBranch = patch.repo.getBranches().findLocalBranch(patch.radProject.defaultBranch);
            if (localDefaultBranch == null) {
                showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"),
                        RadicleBundle.message("mergePatchErrorNoLocalDefault", patch.radProject.defaultBranch));
                return;
            }
            // cleanup
            shouldCleanup = !patch.repo.getStagingAreaHolder().getAllRecords().isEmpty() ||
                    !patch.repo.getUntrackedFilesHolder().retrieveUntrackedFilePaths().isEmpty();
            if (shouldCleanup) {
                boolean stashed = stashChanges();
                if (!stashed) {
                    showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"), RadicleBundle.message("mergePatchErrorStashChanges"));
                    return;
                }
            }
            // check if already in local default branch, otherwise checkout to local default branch
            initialBranch = patch.repo.getCurrentBranch();
            shouldCheckoutBranch = initialBranch == null || !initialBranch.equals(localDefaultBranch);
            if (initialBranch != null && shouldCheckoutBranch) {
                final var trackInfo = patch.repo.getBranchTrackInfo(initialBranch.getName());
                if (trackInfo != null && trackInfo.getRemoteBranch().getNameForLocalOperations().equals(patchRemoteBranchName)) {
                    initialBranchTrackedPatch = true;
                }
            }
            if (shouldCheckoutBranch) {
                boolean ok = checkoutDefaultBranch();
                if (!ok) {
                    showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"),
                            RadicleBundle.message("mergePatchErrorCheckoutDefaultBranch", patch.radProject.defaultBranch));
                    return;
                }
            }

            var opts = showMergeOptionsDialog();
            if (opts == null) {
                return;
            }

            boolean ok = performMerge(opts);
            if (!ok) {
                showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"),
                        RadicleBundle.message("mergePatchErrorMerge", patch.radProject.defaultBranch));
                return;
            }

            if (opts.push) {
                ok = pushChanges();
                if (!ok) {
                    showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"),
                            RadicleBundle.message("mergePatchErrorPush", patch.radProject.defaultBranch));
                }
            }

            // signal that patch model changed, in order to be refreshed
            merged = true;
            logger.warn("merge patch finished");
            patchModel.setValue(patch);
        } catch (Exception e) {
            logger.warn("error merging patch", e);
            showErrorNotification(patch.project, RadicleBundle.message("mergePatchErrorTitle"),
                    RadicleBundle.message("mergePatchErrorMerge", patch.radProject.defaultBranch));
        } finally {
            if (shouldCheckoutBranch && initialBranch != null && !initialBranchTrackedPatch) {
                checkoutInitialBranch();
            }
            if (shouldCleanup) {
                // revert stash
                revertStash();
            }
            if (merged) {
                logger.warn("cleanup after merge patch finished. patch was merged successfully");
            } else {
                logger.warn("cleanup after merge patch finished. patch was not merged due to error, re-enabling merge button");
                ApplicationManager.getApplication().invokeLater(() -> mergeBtn.setEnabled(true));
            }
        }
    }

    protected boolean stashChanges() {
        final var latch = new CountDownLatch(1);
        final var conn = ApplicationManager.getApplication().getMessageBus().connect();
        conn.subscribe(BatchFileChangeListener.TOPIC,
                new ProjectBatchFileChangeListener(patch.project) {
                    @Override
                    public void batchChangeCompleted() {
                        latch.countDown();
                    }
                });

        if (!invokeStashInBackgroundMethod()) {
            shouldCleanup = false;
            conn.disconnect();
            return false;
        }

        boolean stashSuccessful;
        try {
            stashSuccessful = latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("patch merge error waiting for stash");
            stashSuccessful = false;
        }
        if (!stashSuccessful) {
            shouldCleanup = false;
        }
        conn.disconnect();

        return stashSuccessful;
    }

    private boolean invokeStashInBackgroundMethod() {
        /*
        We need to find and invoke the `runStashInBackground` method, which was moved in 2023.3 from GitStash to GitStashOperations
        GitStashOperations.runStashInBackground (2023.3+) vs
        GitStash.runStashInBackground (2023.2-)
        The arguments are the same:
        (patch.project, List.of(patch.repo.getRoot()),
                (vf) -> GitStashUtils.createStashHandler(patch.project, patch.repo.getRoot(), false,
                "stash before merging patch " + patch.id));
        */
        var methodOpt = Stream.concat(
                Arrays.stream(GitStashOperations.class.getDeclaredMethods()),
                Arrays.stream(GitStash.class.getDeclaredMethods())
        ).filter(m -> m.getName().equals("runStashInBackground")).findFirst();
        if (methodOpt.isEmpty()) {
            logger.warn("could not find method runStashInBackground, neither in GitStashOperations nor in GitStash!");
            return false;
        }
        var method = methodOpt.get();
        try {
            method.setAccessible(true);
            method.invoke(null, patch.project, List.of(patch.repo.getRoot()), (Function1<VirtualFile, GitLineHandler>)
                    (vf) -> GitStashUtils.createStashHandler(patch.project, patch.repo.getRoot(), false, "stash before merging patch " + patch.id));
            return true;
        } catch (Exception e) {
            logger.warn("error invoking GitStashOperations.runStashInBackground", e);
            return false;
        }
    }

    protected void revertStash() {
        try {
            var stashes = GitStashUtils.loadStashStack(patch.project, patch.repo.getRoot());
            if (stashes.isEmpty()) {
                return;
            }
            GitStashOperations.unstash(patch.project, stashes.get(0), null, true, false);
        } catch (Exception e) {
            logger.warn("error popping stash", e);
        }
    }

    protected boolean checkoutDefaultBranch() {
        //first get back to the patch target: the default branch
        final var latch = new CountDownLatch(1);
        // taken from GitCheckoutAction
        GitBrancher.getInstance(patch.project).checkout(localDefaultBranch.getName(), false, List.of(patch.repo), latch::countDown);
        boolean ok;
        try {
            ok = latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("patch merge error waiting for checkout to default branch");
            ok = false;
        }
        if (!ok) {
            shouldCheckoutBranch = false;
        }
        return ok;
    }

    protected void checkoutInitialBranch() {
        final var latch = new CountDownLatch(1);
        // taken from GitCheckoutAction
        GitBrancher.getInstance(patch.project).checkout(initialBranch.getName(), false, List.of(patch.repo), latch::countDown);
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("error waiting to checkout back to initial");
        }
    }

    protected MergePatchOptions showMergeOptionsDialog() {
        // no dialog for now
        return new MergePatchOptions(false, true);
    }

    protected boolean performMerge(MergePatchOptions opts) {
        final var latch = new CountDownLatch(1);
        final var success = new AtomicBoolean(false);
        final var error = new AtomicBoolean(false);
        GitVcs.runInBackground(new Task.Backgroundable(patch.project, RadicleBundle.message("Merging patch:" + patch.id), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final var gbui = new GitBranchUiHandlerImpl(patch.project, indicator) {
                    @Override
                    public void notifyError(@NotNull @NlsContexts.NotificationTitle String title, @NotNull @NlsContexts.NotificationContent String message) {
                        error.set(true);
                        logger.warn("error during merge: " + message);
                        super.notifyError(title, message);
                    }
                };
                final var gbw = new GitBranchWorker(patch.project, Git.getInstance(), gbui);
                final var deleteOnMerge = initialBranchTrackedPatch ? GitBrancher.DeleteOnMergeOption.PROPOSE : GitBrancher.DeleteOnMergeOption.NOTHING;
                try {
                    final var repos = List.of(patch.repo);
                    GitReference branch = GitBranchUtil.getCommonLocalBranches(repos).stream().filter(b -> b.getName().equals(patchRemoteBranchName))
                            .findFirst().orElse(null);
                    if (branch == null) {
                        branch = GitBranchUtil.getCommonRemoteBranches(repos).stream().filter(b -> b.getName().equals(patchRemoteBranchName))
                                .findFirst().orElse(null);
                        if (branch == null) {
                            branch = new GitLocalBranch(patchRemoteBranchName);
                        }
                    }
                    gbw.merge(branch, deleteOnMerge, List.of(patch.repo));
                } catch (Exception e) {
                    logger.warn("caught exception during merge", e);
                    error.set(true);
                    success.set(false);
                }
            }

            @Override
            public void onFinished() {
                latch.countDown();
            }

            @Override
            public void onThrowable(Throwable t) {
                error.set(true);
                success.set(false);
                logger.warn("merge on throwable:", t);
            }

            @Override
            public void onSuccess() {
                success.set(true);
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("error waiting for performing merge", e);
        }

        logger.warn("merge performed with error:" + error.get() + " success:" + success.get());
        return !error.get() && success.get();
    }

    protected boolean pushChanges() {
        // Push on success
        var radicleProjectService = patch.project.getService(RadicleProjectService.class);
        var result = radicleProjectService.pushChanges(patch.repo, localDefaultBranch, patchRemote);
        if (result == null) {
            //handle error
            return false;
        }
        var isSuccess = (boolean) result.get("success");
        if (!isSuccess) {
            var message = (String) result.get("message");
            logger.warn("error pushing changes: " + message);
            return false;
        }
        return true;
    }

    public record MergePatchOptions(boolean createMergeCommit, boolean push) { }
}
