package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData;
import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.SelectionSaver;
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
                var data = super.getData(dataId);
                return data != null ? data : VcsTreeModelData.getData(project, this, dataId);
            }
        };
        var pag = new DefaultActionGroup(
                ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON),
                ActionManager.getInstance().getAction("Diff.ShowStandaloneDiff"));
        tree.installPopupHandler(pag);

        /*
        // TODO: this needs to be correctly implemented and setup for diff preview to work as expected
        val diffPreviewController = createAndSetupDiffPreview(tree, diffRequestProducer.changeProducerFactory, dataProvider,
                                                          dataContext.filesManager)
        DataManager.registerDataProvider(parentPanel) { dataId ->
          when {
            EDITOR_TAB_DIFF_PREVIEW.`is`(dataId) -> diffPreviewController.activePreview
            tree.isShowing -> tree.getCustomData(dataId) ?: tree.getData(dataId)
            else -> null
          }
        }
        */

        tree.getEmptyText().setText(emptyText);
        tree.putClientProperty(ExpandableItemsHandler.IGNORE_ITEM_SELECTION, true);
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
            var dataContext = DataManager.getInstance().getDataContext(tree);
            var showDiffAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON);
            var event = AnActionEvent.createFromAnAction(showDiffAction, mouseEvent, ActionPlaces.CHANGES_VIEW_TOOLBAR, dataContext);
            ActionUtil.performActionDumbAwareWithCallbacks(showDiffAction, event);
            return false;
        });
        changesModel.addAndInvokeListener(changes -> {
            tree.rebuildTree();
            return Unit.INSTANCE;
        });
        return tree;
    }
}
