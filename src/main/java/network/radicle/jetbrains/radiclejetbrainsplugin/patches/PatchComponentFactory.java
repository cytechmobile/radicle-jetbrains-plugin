package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.commits.CommitsBrowserComponentBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.DiffPreview;
import com.intellij.openapi.vcs.changes.ui.SimpleAsyncChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.browser.LoadingChangesPanel;
import com.intellij.openapi.vcs.impl.ChangesBrowserToolWindow;
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
import git4idea.GitLocalBranch;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PatchComponentFactory {
    private static final Logger logger = LoggerFactory.getLogger(PatchComponentFactory.class);
    private final RadicleProjectService radicleProjectService;
    private final Project project;
    private final Disposable disposable;
    private final SingleValueModel<List<Change>> patchChanges = new SingleValueModel<>(List.of());
    private final SingleValueModel<List<GitCommit>> patchCommits = new SingleValueModel<>(List.of());
    private JComponent filesComponent;
    private LoadingChangesPanel loadingChangesPanel;
    private JComponent commitBrowser;
    private TabInfo fileTab;
    private TabInfo commitTab;

    public PatchComponentFactory(Project project, Disposable disposer) {
        this.radicleProjectService = project.getService(RadicleProjectService.class);
        this.project = project;
        this.disposable = disposer;
    }

    private void calculatePatchChanges(GitRepository repo, GitLocalBranch localBranch, String head) {
        List<Change> changes = new ArrayList<>();
        var branchRevNumber = this.radicleProjectService.getBranchRevision(repo.getProject(), repo, localBranch.getName());
        final var diff = GitChangeUtils.getDiff(repo, head, branchRevNumber, true);
        if (diff != null && !diff.isEmpty()) {
            changes.addAll(diff);
        }
        changes = changes.stream().distinct().collect(Collectors.toList());
        patchChanges.setValue(changes);
    }

    private void calculatePatchChanges(RadPatch patch) {
        if (patch.revisions.isEmpty()) {
            return;
        }
        var latestRevision = patch.revisions.get(patch.revisions.size() - 1);
        final var diff =  (ArrayList<Change>) GitChangeUtils.getDiff(patch.repo, latestRevision.base(), latestRevision.oid(), true);
        if (diff == null) {
            return;
        }
        patchChanges.setValue(diff);
    }

    private SimpleAsyncChangesBrowser getChangesBrowser() {
        var simpleChangesTree = new SimpleAsyncChangesBrowser(project, false, true);
        DiffPreview diffPreview = ChangesBrowserToolWindow.createDiffPreview(project, simpleChangesTree, disposable);
        simpleChangesTree.setShowDiffActionPreview(diffPreview);
        return simpleChangesTree;
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

    private SimpleAsyncChangesBrowser findChangesBrowser(JComponent component) {
        var changesBrowser = UIUtil.findComponentOfType(component, SimpleAsyncChangesBrowser.class);
        if (changesBrowser == null) {
            logger.warn("Unable to find component");
            return null;
        }
        return changesBrowser;
    }

    private List<GitCommit> calculatePatchCommits(GitRepository repo, String baseRev, String revOid) {
        try {
            return GitHistoryUtils.history(repo.getProject(), repo.getRoot(), baseRev + "..." + revOid);
        } catch (Exception e) {
            logger.warn("error calculating patch commits base rev {} ... rev oid {}", baseRev, revOid, e);
            RadAction.showErrorNotification(repo.getProject(),
                    RadicleBundle.message("radCliError"),
                    RadicleBundle.message("errorCalculatingPatchProposalCommits"));
        }
        return List.of();
    }

    private List<GitCommit> calculatePatchCommits(RadPatch patch) {
        if (patch.revisions.isEmpty()) {
            return List.of();
        }
        var latestRevision = patch.revisions.get(patch.revisions.size() - 1);
        var cmts = calculatePatchCommits(patch.repo, latestRevision.base(), latestRevision.oid());
        Collections.reverse(cmts);
        logger.info("calculated history for patch: {} - {}", patch, cmts);
        return cmts;
    }

    public void updateFilComponent(RadPatch patch) {
        var changesBrowser = findChangesBrowser(filesComponent);
        if (changesBrowser == null) {
            return;
        }
        loadingChangesPanel.loadChangesInBackground(() -> {
            calculatePatchChanges(patch);
            updateTabText(fileTab, RadicleBundle.message("files"), String.valueOf(patchChanges.getValue().size()));
            return true;
        }, success -> changesBrowser.setChangesToDisplay(patchChanges.getValue()));
    }

    public void updateFileComponent(GitRepository repo, GitLocalBranch localBranch, String mainBranchHead) {
        var changesBrowser = findChangesBrowser(filesComponent);
        if (changesBrowser == null) {
            return;
        }
        loadingChangesPanel.loadChangesInBackground(() -> {
            calculatePatchChanges(repo, localBranch, mainBranchHead);
            updateTabText(fileTab, RadicleBundle.message("files"), String.valueOf(patchChanges.getValue().size()));
            return true;
        }, success -> changesBrowser.setChangesToDisplay(patchChanges.getValue()));
    }

    public void updateCommitComponent(GitRepository repo, GitLocalBranch localBranch, String mainBranchHead) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var branchRevNumber =  this.radicleProjectService.getBranchRevision(repo.getProject(), repo, localBranch.getName());
            var changes = calculatePatchCommits(repo, mainBranchHead, branchRevNumber);
            ApplicationManager.getApplication().invokeLater(() -> {
                updateTabText(commitTab, RadicleBundle.message("commits"), String.valueOf(changes.size()));
                patchCommits.setValue(changes);
            }, ModalityState.any());
        });
    }


    public void updateCommitComponent(RadPatch patch) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var changes = calculatePatchCommits(patch);
            ApplicationManager.getApplication().invokeLater(() -> {
                updateTabText(commitTab, RadicleBundle.message("commits"), String.valueOf(changes.size()));
                patchCommits.setValue(changes);
            }, ModalityState.any());
        });
    }

    public JBList<GitCommit> findCommitList() {
        var jbScrollPane = ((JBScrollPane) commitBrowser.getComponents()[1]);
        var jbViewPort =  UIUtil.findComponentOfType(jbScrollPane, JBViewport.class);
        return (JBList<GitCommit>) jbViewPort.getComponents()[0];
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
