package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import kotlin.Unit;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;

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
        //
        //         var changes = createChangesTree();
        //         panel.add(changes);
        //
        //         return panel;
        var panel = new BorderLayoutPanel().withBackground(UIUtil.getListBackground());
        var changes = createChangesTree();
        panel.add(changes);
        return panel;
    }

    private JComponent createChangesTree() {
        var model = new SingleValueModel<>(patch.changes);
        var tree = new PatchProposalChangesTree(patch.repo.getProject(), model).create("empty");

        return ScrollPaneFactory.createScrollPane(tree, false);
    }

    public JComponent createReturnToListSideComponent(PatchTabController controller) {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {controller.createPatchesPanel(); return Unit.INSTANCE;});
    }
}
