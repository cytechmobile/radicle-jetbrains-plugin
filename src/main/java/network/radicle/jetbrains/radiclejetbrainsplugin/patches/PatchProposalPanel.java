package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.collaboration.ui.codereview.commits.CommitsBrowserComponentBuilder;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeActionsToolbarPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.vcs.log.VcsCommitMetadata;
import com.intellij.vcs.log.ui.table.CommitSelectionListener;
import git4idea.GitCommit;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadRemote;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadTrack;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PatchProposalPanel {
    protected RadPatch patch;

    public JComponent createViewPatchProposalPanel(PatchTabController controller, RadPatch patch) {
        this.patch = patch;
        final var uiDisposable = Disposer.newDisposable(controller.getDisposer(), "RadiclePatchProposalDetailsPanel");
        var infoComponent = new JPanel();
        infoComponent.add(new JLabel("INFO COMPONENT\nTODO: IMPLEMENT"));
        var tabInfo = new TabInfo(infoComponent);
        tabInfo.setText(RadicleBundle.message("info"));
        tabInfo.setSideComponent(createReturnToListSideComponent(controller));

        var filesComponent = createFilesComponent();
        var filesInfo = new TabInfo(filesComponent);
        filesInfo.setText(RadicleBundle.message("files"));
        filesInfo.setSideComponent(createReturnToListSideComponent(controller));

        var commitComponent = createCommitComponent();
        commitComponent.add(new JLabel("Commit Component"));
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
        final var model = new SingleValueModel<List<GitCommit>>(Collections.emptyList());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var current = patch.repo.getCurrentRevision();
            try {
                final var history = GitHistoryUtils.history(project, patch.repo.getRoot(),
                        current + ".." + patch.commitHash);
                ApplicationManager.getApplication().invokeLater(() -> model.setValue(history));
            } catch (Exception e) {
                System.err.println("error!!!!");
                e.printStackTrace();
            }
        });

        final var splitter = new OnePixelSplitter(true, "Radicle.PatchProposals.Commits.Component", 0.4f);
        splitter.setOpaque(true);
        splitter.setBackground(UIUtil.getListBackground());
        var actionManager = ActionManager.getInstance();
        // todo: fix. I need to set this to something "listenable" for changes, such as singlevaluemodel
        final AtomicReference<GitCommit> selectedCommit = new AtomicReference<>();
        var commitBrowser = new CommitsBrowserComponentBuilder(project, (SingleValueModel) model)
                .installPopupActions(new DefaultActionGroup(actionManager.getAction("Github.PullRequest.Changes.Reload")), "GHPRCommitsPopup")
                .setEmptyCommitListText(RadicleBundle.message("pull.request.does.not.contain.commits"))
                .onCommitSelected(c -> {selectedCommit.set((GitCommit) c); return Unit.INSTANCE;})
                .create();

        var commitChangesPanel = JBUI.Panels.simplePanel(
                new JLabel("commit changes here")
                //createChangesTree(parent, createCommitChangesModel(model, commitSelectionListener),
                        //GithubBundle.message("pull.request.commit.does.not.contain.changes")))
                )
                .andTransparent();
        commitChangesPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));

        ///val toolbar = GHPRChangesTreeFactory.createTreeToolbar(actionManager, changesLoadingPanel)
        var changesBrowser = new BorderLayoutPanel().andTransparent()
                //.addToTop(toolbar)
                .addToCenter(commitChangesPanel);

        splitter.setFirstComponent(commitBrowser);
        splitter.setSecondComponent(changesBrowser);

        return splitter;
    }

    protected JComponent createFilesComponent() {
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());
        var changes = createChangesTree();
        var actionManager = ActionManager.getInstance();
        var changesToolbarActionGroup = new DefaultActionGroup() {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                final var showDiffAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON);
                return new AnAction[]{showDiffAction};
            }
        };
        var changesToolbar = actionManager.createActionToolbar("ChangesBrowser", changesToolbarActionGroup, true);
        var treeActionsGroup = new DefaultActionGroup(TreeActionsToolbarPanel.createTreeActions(changes));
        var actionsToolbarPanel = new TreeActionsToolbarPanel(changesToolbar, treeActionsGroup, changes);

        return panel.addToTop(actionsToolbarPanel).addToCenter(ScrollPaneFactory.createScrollPane(changes, false));
    }

    protected ChangesTree createChangesTree() {
        final var model = new SingleValueModel<>(patch.changes == null ? Collections.<Change>emptyList() : patch.changes);
        calculatePatchChanges(patch, model);
        return new PatchProposalChangesTree(patch.repo.getProject(), model)
                .create(RadicleBundle.message("emptyChanges"));
    }

    protected void calculatePatchChanges(RadPatch patch, SingleValueModel<List<Change>> model) {
        if (patch.changes != null) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            //first make sure that the peer is tracked
            boolean ok = checkAndTrackPeerIfNeeded(patch);
            if (!ok) {
                patch.changes = null;
                return;
            }
            var computedChanges = GitChangeUtils.getDiff(patch.repo, patch.repo.getCurrentRevision(), patch.commitHash,
                    true);
            patch.changes = computedChanges == null ? Collections.emptyList() : new ArrayList<>(computedChanges);
            ApplicationManager.getApplication().invokeLater(() -> {
                model.setValue(patch.changes);
            });
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
}
