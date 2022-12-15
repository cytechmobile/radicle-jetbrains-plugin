package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.diff.actions.impl.GoToChangePopupBuilder;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.editor.DiffVirtualFile;
import com.intellij.diff.impl.DiffRequestProcessor;

import com.intellij.diff.requests.DiffRequest;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.SplitAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.vcs.log.impl.VcsFileStatusInfo;
import com.vladsch.flexmark.util.sequence.LineInfo;
import git4idea.GitFileRevision;
import git4idea.GitRevisionNumber;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


import static com.intellij.openapi.vcs.history.VcsHistoryUtil.showDiff;

public class PatchProposalDiffVirtualFile extends DiffVirtualFile implements VirtualFilePathWrapper {
    public PatchProposalDiffVirtualFile() {
        super("");
        putUserData(SplitAction.FORBID_TAB_SPLIT,true);
    }

    @Override
    public boolean enforcePresentableName() {
        return true;
    }

    @NotNull
    @Override
    public DiffRequestProcessor createProcessor(@NotNull Project project) {
        List<DiffRequest> requests = new ArrayList<DiffRequest>();
        requests.add(new DiffRequest() {
            @Override
            public @NlsContexts.DialogTitle @Nullable String getTitle() {
                return "das";
            };
        });
        DiffRequestChain requestChain = new SimpleDiffRequestChain(requests);
        var gitRepoManager = GitRepositoryManager.getInstance(project);


        //  GitHistoryUtils.getLastRevision()
        // var number= new GitRevisionNumber("42bdc41ff45068232c57dc8db3ed41ada3a07451");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final FilePath filePath = new LocalFilePath("E:/radicle-jetbrains-plugin/src/main/" +
                        "java/network/radicle/jetbrains/radiclejetbrainsplugin/patches/OpenReviewButton.java",false);
                var currentRevision = GitHistoryUtils.getCurrentRevision(project,filePath,null);
                var number= new GitRevisionNumber(currentRevision.asString());
                final VcsFileRevision compareRevision = new GitFileRevision(project, filePath, number);
                var lastRevision = GitHistoryUtils.getLastRevision(project,filePath);

                ApplicationManager.getApplication().invokeLater(() -> {
                    var k = lastRevision;
                    var t = 5;
                    try {
                        showDiff(project, filePath,compareRevision,compareRevision,"","" );
                    } catch (VcsException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                //  var t= "das";
            } catch (VcsException e) {
                throw new RuntimeException(e);
            }
        });
        // final VcsFileRevision compareRevision = new GitFileRevision(project, filePath, compareRevisionNumber);

        ///   showDiff();
        //GitHistoryUtils.history(project,gitRepoManager.myGlobalRepositoryManager.myPathToRootMap.get("E:/radicle-jetbrains-plugin"))
        //GitHistoryUtils.history()

        //  ProjectLevelVcsManager.getInstance(project).findVcsByName(VcsDataKeys.VCS_FILE_REVISIONS);
        //   GitHistoryUtils.getLastRevision()
        return null;
        //showDiff(project, file,c1,c2,"das","das" );
        // 42bdc41ff45068232c57dc8db3ed41ada3a07451 revision
        //return new GHDiffRequestChainProcessor(project,requestChain);
    }

    @Override
    public @NotNull String getPath() {
        return "C:\\t\\test.txt";
    }

    @Override
    public @NlsSafe @NotNull String getName() {
        return "diff";
    }

    @Override
    public @NotNull String getPresentablePath() {
        return "C:\\t\\test.txt";
    }

    @Override
    public @NotNull @NlsSafe String getPresentableName() {
        return "lakis";
    }
}