package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeActionsToolbarPanel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PatchProposalPanel {
    protected RadPatch patch;

    public JComponent createViewPatchProposalPanel(PatchTabController controller, RadPatch patch) {
        this.patch = patch;
        var uiDisposable = Disposer.newDisposable();
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

        var tabs = new SingleHeightTabs(null,uiDisposable);
        tabs.addTab(tabInfo);
        tabs.addTab(commitInfo);
        tabs.addTab(filesInfo);
        return tabs;
    }

    protected JComponent createCommitComponent() {
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());
        return panel;
    }

    private JComponent createFilesComponent() {
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

    private ChangesTree createChangesTree() {
        var model = new SingleValueModel<>(patch.changes);
        return new PatchProposalChangesTree(patch.repo.getProject(), model).create("empty");
    }

    public JComponent createReturnToListSideComponent(PatchTabController controller) {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {controller.createPatchesPanel(); return Unit.INSTANCE;});
    }
}
