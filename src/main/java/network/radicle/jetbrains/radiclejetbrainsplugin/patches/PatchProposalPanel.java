package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.collaboration.ui.codereview.commits.CommitsBrowserComponentBuilder;
import com.intellij.dvcs.ui.CompareBranchesDiffPanel;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesBrowser;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesBrowserDialogPanel;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.SimpleChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.TreeActionsToolbarPanel;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeListImpl;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadRemote;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PatchProposalPanel {
    private static final Logger logger = LoggerFactory.getLogger(PatchProposalPanel.class);

    protected RadPatch patch;
    protected final SingleValueModel<List<Change>> patchChanges = new SingleValueModel<>(List.of());
    protected final SingleValueModel<List<GitCommit>> patchCommits = new SingleValueModel<>(List.of());

    public JComponent createViewPatchProposalPanel(PatchTabController controller, RadPatch patch) {
        this.patch = patch;
        this.patchChanges.setValue(List.of());
        this.patchCommits.setValue(List.of());

        calculatePatchChanges();
        calculatePatchCommits();

        final var uiDisposable = Disposer.newDisposable(controller.getDisposer(), "RadiclePatchProposalDetailsPanel");
        var infoComponent = new JPanel();
        infoComponent.add(new JLabel("<html>INFO COMPONENT<br>TODO: IMPLEMENT</html>"));
        var tabInfo = new TabInfo(infoComponent);
        tabInfo.setText(RadicleBundle.message("info"));
        tabInfo.setSideComponent(createReturnToListSideComponent(controller));

        var filesComponent = createFilesComponent();
        var filesInfo = new TabInfo(filesComponent);
        filesInfo.setText(RadicleBundle.message("files"));
        filesInfo.setSideComponent(createReturnToListSideComponent(controller));

        var commitComponent = createCommitComponent();
        var commitInfo = new TabInfo(commitComponent);
        commitInfo.setText(RadicleBundle.message("commits"));
        commitInfo.setSideComponent(createReturnToListSideComponent(controller));

        var tabs = new SingleHeightTabs(null, uiDisposable);
        tabs.addTab(tabInfo);
        tabs.addTab(filesInfo);
        tabs.addTab(commitInfo);
        return tabs;
    }

    protected JComponent createReturnToListSideComponent(PatchTabController controller) {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {controller.createPatchesPanel(); return Unit.INSTANCE;});
    }

    protected JComponent createCommitComponent() {
        final var project = patch.repo.getProject();
        var simpleChangesTree = new SimpleChangesBrowser(patch.repo.getProject(),List.of());
        final var splitter = new OnePixelSplitter(true, "Radicle.PatchProposals.Commits.Component", 0.4f);
        splitter.setOpaque(true);
        splitter.setBackground(UIUtil.getListBackground());
        final SingleValueModel<List<Change>> selectedCommitChanges = new SingleValueModel<>(List.of());
        var commitBrowser = new CommitsBrowserComponentBuilder(project, (SingleValueModel) patchCommits)
                .setEmptyCommitListText(RadicleBundle.message("patchProposalNoCommits"))
                .onCommitSelected(c -> {
                    if (c == null || ! (c instanceof GitCommit gc)) {
                        selectedCommitChanges.setValue(List.of());
                    } else {
                        selectedCommitChanges.setValue(new ArrayList<>(gc.getChanges()));
                    }
                    simpleChangesTree.setChangesToDisplay(selectedCommitChanges.getValue());
                    return Unit.INSTANCE;
                })
                .create();

        //  var commitChangesTree = new PatchProposalChangesTree(patch.repo.getProject(), selectedCommitChanges)
        //          .create(RadicleBundle.message("emptyChanges"));

        //  var commitChangesPanel = JBUI.Panels.simplePanel(commitChangesTree).andTransparent()
        //          .withBorder(IdeBorderFactory.createBorder(SideBorder.TOP));

        // var toolbar = createChangesTreeActionToolbar(commitChangesTree);


        //   var changesBrowser = new BorderLayoutPanel().andTransparent().addToTop(toolbar)
        //          .addToCenter(ScrollPaneFactory.createScrollPane(commitChangesPanel, true));

        //  CommittedChangesBrowser myChangesBrowser = new CommittedChangesBrowser(patch.repo.getProject());
        // myChangesBrowser.setChangesToDisplay(selectedCommitChanges.getValue());

        splitter.setFirstComponent(commitBrowser);
        splitter.setSecondComponent(simpleChangesTree);

        return splitter;
    }

    protected JComponent createFilesComponent() {
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());
        var changes = new PatchProposalChangesTree(patch.repo.getProject(), patchChanges)
                .create(RadicleBundle.message("emptyChanges"));
        var actionsToolbarPanel = createChangesTreeActionToolbar(changes);

        return panel.addToTop(actionsToolbarPanel).addToCenter(ScrollPaneFactory.createScrollPane(changes, true));
    }

    protected void calculatePatchChanges() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            //first make sure that the peer is tracked
            boolean ok = checkAndTrackPeerIfNeeded(patch);
            if (!ok) {
                return;
            }
            var diff = GitChangeUtils.getDiff(patch.repo, patch.repo.getCurrentRevision(), patch.commitHash, true);
            final List<Change> changes = diff == null ? Collections.emptyList() : new ArrayList<>(diff);
            ApplicationManager.getApplication().invokeLater(() -> {
                patchChanges.setValue(changes);
            });
        });
    }

    protected void calculatePatchCommits() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var current = patch.repo.getCurrentRevision();
            try {
                final var history = GitHistoryUtils.history(patch.repo.getProject(), patch.repo.getRoot(),
                        patch.commitHash + "..." + current);
                logger.info("calculated history for patch: {} - ({}..{}) {}", patch, patch.commitHash, current, history);
                ApplicationManager.getApplication().invokeLater(() -> patchCommits.setValue(history));
            } catch (Exception e) {
                logger.warn("error calculating patch commits for patch: {}", patch, e);
                RadAction.showErrorNotification(patch.repo.getProject(),
                        RadicleBundle.message("radCliError" ),
                        RadicleBundle.message("errorCalculatingPatchProposalCommits"));
            }
        });
    }

    protected boolean checkAndTrackPeerIfNeeded(RadPatch patch) {
        if (patch.self) {
            return true;
        }
        final var trackedPeers = new RadRemote(patch.repo).findTrackedPeers();
        if (trackedPeers != null && !trackedPeers.isEmpty()) {
            var tracked = trackedPeers.stream().filter(p -> p.id().equals(patch.peerId)).findAny().orElse(null);
            if (tracked != null) {
                return true;
            }
        }

        var trackPeer = new RadTrack(patch.repo, new RadTrack.Peer(patch.peerId));
        var out = trackPeer.perform();
        return RadTrack.isSuccess(out);
    }

    protected JPanel createChangesTreeActionToolbar(ChangesTree tree) {
        final var actionManager = ActionManager.getInstance();
        var changesToolbarActionGroup = new DefaultActionGroup(
                actionManager.getAction(IdeActions.ACTION_SHOW_DIFF_COMMON),
                actionManager.getAction("Diff.ShowStandaloneDiff"),
                actionManager.getAction(ChangesTree.GROUP_BY_ACTION_GROUP));
        var changesToolbar = actionManager.createActionToolbar("RadicleChangesBrowser", changesToolbarActionGroup, true);
        return new TreeActionsToolbarPanel(changesToolbar, tree);
    }
}
