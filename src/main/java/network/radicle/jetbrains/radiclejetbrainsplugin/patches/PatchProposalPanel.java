package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.CurrentBranchComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.intellij.util.ui.UIUtil;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import icons.CollaborationToolsIcons;
import icons.DvcsImplIcons;
import kotlin.Unit;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.UpdateBackgroundTask;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadCheckout;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.LabeledListPanelHandle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.PopupBuilder;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SelectionListCellRenderer;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils.formatPatchId;

public class PatchProposalPanel {
    private static final Logger logger = Logger.getInstance(PatchProposalPanel.class);

    protected RadPatch patch;
    protected SingleValueModel<RadPatch> patchModel;
    protected TabInfo commitTab;
    protected PatchTabController controller;
    private SingleHeightTabs tabs;
    private final RadicleProjectApi api;
    private final LabelSelect labelSelect;
    private final StateSelect stateSelect;
    private final PatchComponentFactory patchComponentFactory;
    private final MessageBusConnection messageBusConnection;
    private JButton checkoutBtn;
    private JButton mergeBtn;

    public PatchProposalPanel(PatchTabController controller, SingleValueModel<RadPatch> patch) {
        this.controller = controller;
        this.patch = patch.getValue();
        this.api = this.patch.project.getService(RadicleProjectApi.class);
        this.patchModel = patch;
        this.labelSelect = new LabelSelect();
        this.stateSelect = new StateSelect();
        this.patchComponentFactory = new PatchComponentFactory(patch.getValue().project, this.controller.getDisposer(), patch.getValue());
        this.messageBusConnection = this.patch.project.getMessageBus().connect();
        this.listenForRepoChanges();
    }

    private void listenForRepoChanges() {
        messageBusConnection.subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> {
                    var currentBranch = patch.repo.getCurrentBranch();
                    if (currentBranch == null) {
                        return;
                    }
                    var currentBranchName = patch.repo.getCurrentBranch().getName();
                    checkoutBtn.setEnabled(!currentBranchName.contains(formatPatchId(patch.id)));
                });
    }

    public JComponent createViewPatchProposalPanel() {
        final var uiDisposable = Disposer.newDisposable(controller.getDisposer(), "RadiclePatchProposalDetailsPanel");
        var infoComponent = createInfoComponent();
        var tabInfo = new TabInfo(infoComponent);
        tabInfo.setText(RadicleBundle.message("info"));
        tabInfo.setSideComponent(createReturnToListSideComponent());

        var filesComponent = patchComponentFactory.createFilesComponent();
        var filesInfo = new TabInfo(filesComponent);
        filesInfo.setText(RadicleBundle.message("files"));
        filesInfo.setSideComponent(createReturnToListSideComponent());
        patchComponentFactory.setFileTab(filesInfo);
        patchComponentFactory.updateFilComponent(patch);

        var commitComponent = patchComponentFactory.createCommitComponent();
        commitTab = new TabInfo(commitComponent);
        commitTab.setText(RadicleBundle.message("commits"));
        commitTab.setSideComponent(createReturnToListSideComponent());
        patchComponentFactory.setCommitTab(commitTab);
        patchComponentFactory.updateCommitComponent(patch);

        tabs = new SingleHeightTabs(null, uiDisposable);
        tabs.addTab(tabInfo);
        tabs.addTab(filesInfo);
        tabs.addTab(commitTab);
        return tabs;
    }

    protected JComponent createReturnToListSideComponent() {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {
                    this.messageBusConnection.disconnect();
                    controller.createPanel();
                    return Unit.INSTANCE;
                });
    }

    protected JComponent createInfoComponent() {
        var branchPanel = branchComponent();
        var titlePanel = titleComponent();
        var descriptionPanel = descriptionComponent();

        var detailsSection = new JPanel(new MigLayout(new LC()
                .insets("0", "0", "0", "0").gridGap("0", "0").fill().flowY()));
        detailsSection.setOpaque(false);
        detailsSection.setBorder(JBUI.Borders.empty(8));

        detailsSection.add(branchPanel, new CC().gapBottom(String.valueOf(UI.scale(8))));
        detailsSection.add(titlePanel, new CC().gapBottom(String.valueOf(UI.scale(8))));
        detailsSection.add(descriptionPanel, new CC().gapBottom(String.valueOf(UI.scale(8))));

        var borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(detailsSection, BorderLayout.NORTH);

        var actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        Utils.addListPanel(actionPanel, stateSelect);
        Utils.addListPanel(actionPanel, labelSelect);
        if (RadPatch.State.OPEN.status.equals(patch.state.status)) {
            mergeBtn = new JButton();
            // current user is a delegate in the project
            if (patch.radProject.delegates != null && patch.radProject.delegates.stream().anyMatch(d -> d.id.contains(patch.self.id))) {
                final var mergePatchAction = new MergePatchAction(mergeBtn, patchModel);
                mergeBtn.setAction(mergePatchAction);
            } else {
                mergeBtn.setEnabled(false);
                mergeBtn.setToolTipText(RadicleBundle.message("mergeDisabledNotDelegate"));
            }
            mergeBtn.setText(RadicleBundle.message("merge"));
            actionPanel.add(mergeBtn);
        } else {
            mergeBtn = null;
        }
        final var splitter = new OnePixelSplitter(true, "Radicle.PatchPanel.action.Component", 0.6f);
        splitter.setFirstComponent(borderPanel);
        splitter.setSecondComponent(actionPanel);

        return splitter;
    }

    private JComponent descriptionComponent() {
        var titlePane = new BaseHtmlEditorPane();
        //titlePane.setFont(titlePane.getFont().deriveFont((float) (titlePane.getFont().getSize() * 1.2)));
        //HtmlEditorPaneUtilKt.setHtmlBody(titlePane, patch.getLatestNonEmptyRevisionDescription());
        titlePane.setBody(patch.getLatestNonEmptyRevisionDescription());
        var nonOpaquePanel = new NonOpaquePanel(new MigLayout(new LC().insets("0").gridGap("0", "0").noGrid().flowY()));
        nonOpaquePanel.add(titlePane, new CC().gapBottom(String.valueOf(UI.scale(8))));
        final var viewTimelineLink = new ActionLink(RadicleBundle.message("view.timeline"), e -> {
            controller.openPatchTimelineOnEditor(patchModel, this, false);
        });
        viewTimelineLink.setBorder(JBUI.Borders.emptyTop(4));
        nonOpaquePanel.add(viewTimelineLink, new CC().gapBottom(String.valueOf(UI.scale(8))));

        var checkoutAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkoutBtn.setEnabled(false);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    var formattedPatchId = formatPatchId(patch.id);
                    var countDown = new CountDownLatch(1);
                    UpdateBackgroundTask ubt = new UpdateBackgroundTask(patch.project, RadicleBundle.message("checkingOut", "patch/" + formattedPatchId),
                            countDown);
                    new Thread(ubt::queue).start();
                    var radCheckout = new RadCheckout(patch.repo, patch.id);
                    var output = radCheckout.perform();
                    if (!RadAction.isSuccess(output)) {
                        var errorMsg = output.getStdout();
                        RadAction.showErrorNotification(patch.project, "", errorMsg);
                        countDown.countDown();
                        ApplicationManager.getApplication().invokeLater(() -> checkoutBtn.setEnabled(true));
                        return;
                    }
                    refreshVcs();
                    countDown.countDown();
                    RadAction.showNotification(patch.project, "", RadicleBundle.message("successCheckingOut", formattedPatchId),
                            NotificationType.INFORMATION, List.of());
                });
            }
        };
        checkoutBtn = new JButton(checkoutAction);
        checkoutBtn.setText(RadicleBundle.message("checkout"));
        var currentBranch = patch.repo.getCurrentBranch();
        if (currentBranch != null && currentBranch.getName().contains(formatPatchId(patch.id))) {
            checkoutBtn.setEnabled(false);
        }
        nonOpaquePanel.add(checkoutBtn, new CC().gapBottom(String.valueOf(UI.scale(8))));
        return nonOpaquePanel;
    }

    public void refreshVcs() {
        // Refresh repo and root directory
        patch.repo.update();
        GitUtil.refreshVfs(patch.repo.getRoot(), null);
    }

    private JComponent titleComponent() {
        final var icon = new JLabel(CollaborationToolsIcons.PullRequestOpen);

        final var titlePane = new BaseHtmlEditorPane();
        titlePane.setFont(titlePane.getFont().deriveFont((float) (titlePane.getFont().getSize() * 1.2)));
        titlePane.setBody(patch.title);

        final var iconAndTitlePane = new NonOpaquePanel(new MigLayout(new LC().insets("0").gridGap("0", "0").noGrid()));
        iconAndTitlePane.add(icon, new CC().gapRight(String.valueOf(JBUIScale.scale(4))));
        iconAndTitlePane.add(titlePane, new CC().gapRight(String.valueOf(JBUIScale.scale(4))));

        final var idPane = new BaseHtmlEditorPane();
        idPane.setFont(titlePane.getFont().deriveFont((float) (titlePane.getFont().getSize() * 0.8)));
        idPane.setForeground(JBColor.GRAY);
        idPane.setBody(patch.id);

        final var nonOpaquePanel = new NonOpaquePanel(new MigLayout(new LC().insets("0").gridGap("0", "0").fill().flowY()));
        nonOpaquePanel.add(iconAndTitlePane, new CC().gapBottom(String.valueOf(UI.scale(8))));
        nonOpaquePanel.add(idPane, new CC());
        return nonOpaquePanel;
    }

    private JComponent branchComponent() {
        var branchPanel = new NonOpaquePanel();
        branchPanel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        var to = createLabel(patch.radProject.defaultBranch);
        var revision = patch.revisions.get(patch.revisions.size() - 1);
        var ref = !revision.refs().isEmpty() ? revision.refs().get(revision.refs().size() - 1) : "ref/head/patches/" + patch.id;
        var from = createLabel(ref);
        branchPanel.add(to, new CC().minWidth(Integer.toString(JBUIScale.scale(30))));
        var arrowLabel = new JLabel(UIUtil.leftArrow());
        arrowLabel.setForeground(CurrentBranchComponent.TEXT_COLOR);
        arrowLabel.setBorder(JBUI.Borders.empty(0, 5));
        branchPanel.add(arrowLabel);
        branchPanel.add(from, new CC().minWidth(Integer.toString(JBUIScale.scale(30))));
        return branchPanel;
    }

    private JBLabel createLabel(String branchName) {
        var label = new JBLabel(DvcsImplIcons.BranchLabel);
        CollaborationToolsUIUtil.INSTANCE.overrideUIDependentProperty(label, jbLabel -> {
            jbLabel.setForeground(CurrentBranchComponent.TEXT_COLOR);
            jbLabel.setBackground(CurrentBranchComponent.getBranchPresentationBackground(UIUtil.getListBackground()));
            jbLabel.andOpaque();
            return null;
        });
        label.setText(branchName);
        return label;
    }

    public LabelSelect getLabelSelect() {
        return labelSelect;
    }

    public StateSelect getStateSelect() {
        return stateSelect;
    }

    public PatchTabController getController() {
        return controller;
    }

    public void selectCommit(String oid) {
        tabs.select(commitTab, true);
        var list = patchComponentFactory.findCommitList();
        var index = -1;
        for (var i = 0; i < list.getItemsCount(); i++) {
            var item = list.getModel().getElementAt(i);
            if (oid.contains(item.getId().asString())) {
                index = i;
                break;
            }
        }
        list.setSelectedIndex(index);
    }

    public class StateSelect extends LabeledListPanelHandle<StateSelect.State> {

        public record State(String status, String label) {
        }

        public StateSelect() {
            if (patch.isMerged()) {
                disableEditButton(RadicleBundle.message("patchStateChangeTooltip"));
            }
        }

        public static class StateRender extends SelectionListCellRenderer<State> {

            @Override
            public String getText(PatchProposalPanel.StateSelect.State value) {
                return value.label;
            }

            @Override
            public String getPopupTitle() {
                return RadicleBundle.message("state");
            }
        }

        @Override
        public String getSelectedValues() {
            return patch.state.label;
        }

        @Override
        public boolean storeValues(List<State> data) {
            var selectedState = data.get(0).status;
            // We don't have changes so don't refresh the window
            if (selectedState.equals(patch.state.status)) {
                return true;
            }
            var resp = api.changePatchState(patch, selectedState);
            var isSuccess = resp != null;
            if (isSuccess) {
                patchModel.setValue(patch);
            }
            return isSuccess;
        }


        @Override
        public SelectionListCellRenderer<PatchProposalPanel.StateSelect.State> getRender() {
            return new PatchProposalPanel.StateSelect.StateRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<State>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var allStates = Arrays.stream(RadPatch.State.values()).filter(e -> !Objects.equals(e.status, RadPatch.State.MERGED.status))
                        .map(e -> new PatchProposalPanel.StateSelect.State(e.status, e.label)).toList();
                var stateList = new ArrayList<SelectionListCellRenderer.SelectableWrapper<State>>();
                for (PatchProposalPanel.StateSelect.State state : allStates) {
                    var isSelected = patch.state.status.equals(state.status);
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(state, isSelected);
                    stateList.add(selectableWrapper);
                }
                return stateList;
            });
        }

        @Override
        public String getLabel() {
            return RadicleBundle.message("state");
        }

        @Override
        public boolean isSingleSelection() {
            return true;
        }
    }

    public class LabelSelect extends LabeledListPanelHandle<LabelSelect.Label> {

        public record Label(String label) {
        }

        public static class LabelRender extends SelectionListCellRenderer<LabelSelect.Label> {

            @Override
            public String getText(LabelSelect.Label value) {
                return value.label;
            }

            @Override
            public String getPopupTitle() {
                return RadicleBundle.message("label");
            }
        }

        @Override
        public String getSelectedValues() {
            return String.join(",", patch.labels);
        }

        @Override
        public boolean storeValues(List<Label> labels) {
            var labelList = labels.stream().map(value -> value.label).toList();
            // We don't have changes so don't refresh the window
            if (labelList.size() == patch.labels.size()) {
                return true;
            }
            var resp = api.addRemovePatchLabel(patch, labelList);
            var isSuccess = resp != null;
            if (isSuccess) {
                patchModel.setValue(patch);
            }
            return isSuccess;
        }

        @Override
        public CompletableFuture<List<LabelSelect.Label>> showEditPopup(JComponent parent) {
            var addField = new JBTextField();
            var res = new CompletableFuture<List<LabelSelect.Label>>();
            var popUpBuilder = new PopupBuilder();
            jbPopup = popUpBuilder.createPopup(this.getData(), getRender(), this.isSingleSelection(), addField, res);
            jbPopup.showUnderneathOf(parent);
            listener = popUpBuilder.getListener();
            latch = popUpBuilder.getLatch();
            return res.thenApply(data -> {
                if (Strings.isNullOrEmpty(addField.getText())) {
                    return data;
                }
                var myList = new ArrayList<>(data);
                myList.add(new LabelSelect.Label(addField.getText()));
                return myList;
            });
        }

        @Override
        public SelectionListCellRenderer<LabelSelect.Label> getRender() {
            return new LabelRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<Label>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var labelFuture = new ArrayList<SelectionListCellRenderer.SelectableWrapper<Label>>();
                for (String label : patch.labels) {
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(new LabelSelect.Label(label), true);
                    labelFuture.add(selectableWrapper);
                }
                return labelFuture;
            });
        }

        @Override
        public String getLabel() {
            return RadicleBundle.message("label");
        }

        @Override
        public boolean isSingleSelection() {
            return false;
        }
    }
}
