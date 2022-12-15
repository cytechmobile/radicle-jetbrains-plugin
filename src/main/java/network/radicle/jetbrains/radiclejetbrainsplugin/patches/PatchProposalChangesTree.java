package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData;
import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.SelectionSaver;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PatchProposalChangesTree {
    private Project project;
    private SingleValueModel<Collection<Change>> changesModel;

    public PatchProposalChangesTree(Project project, SingleValueModel<Collection<Change>> changesModel) {
        this.project = project;
        this.changesModel = changesModel;
    }

    public ChangesTree create(String emptyText) {
        var tree = new ChangesTree(project,false,false){
            @Override
            public void rebuildTree() {
                updateTreeModel(new TreeModelBuilder(project,getGrouping())
                        .setChanges(changesModel.getValue(),null).build());
                if (isSelectionEmpty() && !isEmpty()) {
                    TreeUtil.selectFirstNode(this);
                }
            }

            @Override
            public @Nullable Object getData(@NotNull String dataId) {
                var k = VcsTreeModelData.getData(project,this,dataId);
                return k;
            }
        };
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
        changesModel.addAndInvokeListener(new Function1<>() {
            @Override
            public Unit invoke(Collection<Change> changes) {
                tree.rebuildTree();
                return Unit.INSTANCE;
            }
        });
        return tree;
    }

}
