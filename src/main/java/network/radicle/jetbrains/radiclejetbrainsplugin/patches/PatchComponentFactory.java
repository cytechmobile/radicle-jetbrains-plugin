package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.commits.CommitsBrowserComponentBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.SimpleAsyncChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.SimpleTreeDiffRequestProcessor;
import com.intellij.openapi.vcs.changes.ui.SimpleTreeEditorDiffPreview;
import com.intellij.openapi.vcs.changes.ui.browser.LoadingChangesPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

public class PatchComponentFactory {
    public static final Key<RadPatch> PATCH_DIFF = Key.create("Patch.Diff");
    private static final Logger logger = LoggerFactory.getLogger(PatchComponentFactory.class);
    private final RadicleProjectService radicleProjectService;
    private final Project project;
    private final Disposable disposable;
    private final RadPatch myPatch;
    private final SingleValueModel<List<Change>> patchChanges = new SingleValueModel<>(List.of());
    private final SingleValueModel<List<GitCommit>> patchCommits = new SingleValueModel<>(List.of());
    private JComponent filesComponent;
    private LoadingChangesPanel loadingChangesPanel;
    private JComponent commitBrowser;
    private TabInfo fileTab;
    private TabInfo commitTab;

    public PatchComponentFactory(Project project, Disposable disposer) {
        this(project, disposer, null);
    }

    public PatchComponentFactory(Project project, Disposable disposer, RadPatch patch) {
        this.radicleProjectService = project.getService(RadicleProjectService.class);
        this.project = project;
        this.disposable = disposer;
        this.myPatch = patch;
    }

    public JComponent createFilesComponent() {
        var simpleChangesTree = getChangesBrowser();
        loadingChangesPanel = new LoadingChangesPanel(simpleChangesTree, simpleChangesTree.getViewer().getEmptyText(), disposable);
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());
        filesComponent = panel.addToCenter(ScrollPaneFactory.createScrollPane(loadingChangesPanel, true));
        return filesComponent;
    }

    public JComponent createCommitComponent() {
        var simpleChangesTree = getChangesBrowser();
        final var splitter = new OnePixelSplitter(true, "Radicle.PatchProposals.Commits.Component", 0.4f);
        splitter.setOpaque(true);
        splitter.setBackground(UIUtil.getListBackground());
        final SingleValueModel<List<Change>> selectedCommitChanges = new SingleValueModel<>(List.of());
        commitBrowser = new CommitsBrowserComponentBuilder(project, (SingleValueModel) patchCommits)
                .setEmptyCommitListText(RadicleBundle.message("patchProposalNoCommits"))
                .onCommitSelected(c -> {
                    if (c == null || !(c instanceof GitCommit gc)) {
                        selectedCommitChanges.setValue(List.of());
                    } else {
                        selectedCommitChanges.setValue(new ArrayList<>(gc.getChanges()));
                    }
                    simpleChangesTree.setChangesToDisplay(selectedCommitChanges.getValue());
                    return Unit.INSTANCE;
                })
                .create();

        splitter.setFirstComponent(commitBrowser);
        splitter.setSecondComponent(simpleChangesTree);
        return splitter;
    }

    public void updateFileAndCommitComponents(GitRepository repo, String revNumber, String mainBranchHead, int retriesAttempt) {
        radicleProjectService.executeInBackground(() -> {
            try {
                var changes = calculatePatchCommits(repo, mainBranchHead, revNumber);
                calculatePatchChanges(repo, mainBranchHead, revNumber);
                ApplicationManager.getApplication().invokeLater(() -> {
                    updateFileComponent();
                    updateCommitComponent(changes);
                });
            } catch (VcsException e) {
                if (retriesAttempt > 0) {
                    radicleProjectService.fetchPeerChanges(repo);
                    updateFileAndCommitComponents(repo, mainBranchHead, revNumber, retriesAttempt - 1);
                }
            }
        });
    }

    public void updateFileAndCommitComponents(RadPatch patch) {
        updateFileAndCommitComponents(patch.repo, patch.getLatestRevision().oid(), patch.getLatestRevision().base(), 1);
    }

    public JBList<GitCommit> findCommitList() {
        var jbScrollPane = ((JBScrollPane) commitBrowser.getComponents()[1]);
        var jbViewPort =  UIUtil.findComponentOfType(jbScrollPane, JBViewport.class);
        return (JBList<GitCommit>) jbViewPort.getComponents()[0];
    }

    private SimpleAsyncChangesBrowser getChangesBrowser() {
        var simpleChangesTree = new SimpleAsyncChangesBrowser(project, false, true);
        var diffRequestProcessor = new SimpleTreeDiffRequestProcessor(project, "ChangesToolWindowPreview", simpleChangesTree.getViewer(), disposable);
        var diffContext = diffRequestProcessor.getContext();
        diffContext.putUserData(PATCH_DIFF, myPatch);
        var simpleTreeEditorDiffPreview = new SimpleTreeEditorDiffPreview(diffRequestProcessor, simpleChangesTree.getViewer()) {
            @Nullable
            @Override
            protected String getCurrentName() {
                return myPatch != null ? RadicleBundle.message("patch.diff.label", Utils.formatId(myPatch.id)) :
                        RadicleBundle.message("changes");
            }
        };
        simpleChangesTree.setShowDiffActionPreview(simpleTreeEditorDiffPreview);
        return simpleChangesTree;
    }

    private SimpleAsyncChangesBrowser findChangesBrowser(JComponent component) {
        var changesBrowser = UIUtil.findComponentOfType(component, SimpleAsyncChangesBrowser.class);
        if (changesBrowser == null) {
            logger.warn("Unable to find component");
            return null;
        }
        return changesBrowser;
    }

    private List<Change> calculatePatchChanges(GitRepository repo, String oldRev, String newRev) throws VcsException {
        final var diff =  (ArrayList<Change>) GitChangeUtils.getDiff(repo, oldRev, newRev, true);
        if (diff == null) {
            throw new VcsException("Couldn't collect changes between " + oldRev + " and " + newRev);
        }
        List<Change> changes = new ArrayList<>(diff);
        patchChanges.setValue(changes);
        return diff;
    }

    private void updateFileComponent() {
        var changesBrowser = findChangesBrowser(filesComponent);
        if (changesBrowser == null) {
            return;
        }
        updateTabText(fileTab, RadicleBundle.message("files"), String.valueOf(patchChanges.getValue().size()));
        changesBrowser.setChangesToDisplay(patchChanges.getValue());
    }

    private List<GitCommit> calculatePatchCommits(GitRepository repo, String baseRev, String revOid) throws VcsException {
        return GitHistoryUtils.history(repo.getProject(), repo.getRoot(), baseRev + "..." + revOid);
    }

    private void updateCommitComponent(List<GitCommit> changes) {
        updateTabText(commitTab, RadicleBundle.message("commits"), String.valueOf(changes.size()));
        patchCommits.setValue(changes);
    }

    public void setCommitTab(TabInfo tab) {
        this.commitTab = tab;
    }

    public void setFileTab(TabInfo tab) {
        this.fileTab = tab;
    }

    private void updateTabText(TabInfo tab, String title, String numOfChanges) {
        if (tab == null) {
            return;
        }
        tab.clearText(false);
        tab.setText(title);
        tab.append(" " + numOfChanges, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
    }
}
