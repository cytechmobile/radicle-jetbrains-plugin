package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData;
import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.SelectionSaver;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

public class PatchProposalChangesTree {
    private Project project;
    private SingleValueModel<List<Change>> changesModel;

    public PatchProposalChangesTree(Project project, SingleValueModel<List<Change>> changesModel) {
        this.project = project;
        this.changesModel = changesModel;
    }

    public ChangesTree create(String emptyText) {
        var tree = new ChangesTree(project, false, false) {
            @Override
            public void rebuildTree() {
                updateTreeModel(new TreeModelBuilder(project, getGrouping()).setChanges(changesModel.getValue(), null)
                        .build());
                if (isSelectionEmpty() && !isEmpty()) {
                    TreeUtil.selectFirstNode(this);
                }
            }

            @Override
            public @Nullable Object getData(@NotNull String dataId) {
                return VcsTreeModelData.getData(project, this, dataId);
            }
        };

        final var actionGroup = new DefaultActionGroup();
        final var showDiffAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON);
        showDiffAction.registerCustomShortcutSet(showDiffAction.getShortcutSet(), tree);
        actionGroup.add(showDiffAction);

        var toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, actionGroup, false);
        toolbar.setTargetComponent(tree);

        PopupHandler.installPopupMenu(tree, actionGroup, ActionPlaces.CHANGES_VIEW_POPUP);
        tree.getEmptyText().setText(emptyText);
        UIUtil.putClientProperty(tree, ExpandableItemsHandler.IGNORE_ITEM_SELECTION, true);
        SelectionSaver.installOn(tree);
        tree.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tree.isSelectionEmpty() && tree.isEmpty()) {
                    TreeUtil.selectFirstNode(tree);
                }
            }
        });
        tree.setDoubleClickHandler(mouseEvent -> {
            System.out.println("dblclick: " + mouseEvent);
            var sel = tree.getSelectionRows();
            if (sel != null && sel.length > 0) {
                var selectedIdx = sel[0];
                var selected = changesModel.getValue().get(selectedIdx);
                System.out.println("selected change is: " + selected);
            }
            return true;
        });
        changesModel.addAndInvokeListener(changes -> {
            tree.rebuildTree();
            return Unit.INSTANCE;
        });
        return tree;
    }

}
