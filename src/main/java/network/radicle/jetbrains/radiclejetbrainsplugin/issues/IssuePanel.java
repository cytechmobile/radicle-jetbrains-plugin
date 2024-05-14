package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import kotlin.Unit;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.LabeledListPanelHandle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.PopupBuilder;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SelectionListCellRenderer;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;


import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IssuePanel {
    protected RadIssue issue;
    protected SingleValueModel<RadIssue> issueModel;
    protected TabInfo infoTab;
    protected IssueTabController issueTabController;
    private final RadicleProjectApi api;
    private final AssigneesSelect assigneesSelect;
    private final StateSelect stateSelect;
    private final LabelSelect labelSelect;
    private static final String PATTERN_FORMAT = "dd/MM/yyyy HH:mm";
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    public IssuePanel(IssueTabController issueTabController, SingleValueModel<RadIssue> issueModel) {
        this.issueTabController = issueTabController;
        this.issueModel = issueModel;
        this.issue = issueModel.getValue();
        this.api = issue.project.getService(RadicleProjectApi.class);
        this.assigneesSelect = new AssigneesSelect();
        this.stateSelect = new StateSelect();
        this.labelSelect = new LabelSelect();
    }

    public JComponent createPanel() {
        var component = getComponent();
        infoTab = new TabInfo(component);
        infoTab.setText(RadicleBundle.message("issueOverview"));
        infoTab.setSideComponent(createReturnToListSideComponent());

        final var uiDisposable = Disposer.newDisposable(issueTabController.getDisposer(),
                "RadicleIssuePanel");
        SingleHeightTabs tabs = new SingleHeightTabs(null, uiDisposable);
        tabs.addTab(infoTab);
        return tabs;
    }

    private JComponent getComponent() {
        var actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        addListPanel(actionPanel, assigneesSelect);
        addListPanel(actionPanel, stateSelect);
        addListPanel(actionPanel, labelSelect);
        final var splitter = new OnePixelSplitter(true, "Radicle.IssuePanel.action.Component", 0.6f);
        splitter.setFirstComponent(getIssueInfo());
        splitter.setSecondComponent(actionPanel);
        return splitter;
    }

    private JComponent getIssueInfo() {
        var detailsSection = new JPanel(new MigLayout(new LC()
                .insets("0", "5", "0", "0").gridGap("0", "0").fill().flowY()));
        detailsSection.setOpaque(false);
        detailsSection.setBorder(JBUI.Borders.empty(8));

        var issueTitle = getLabelPanel(RadicleBundle.message("title", Strings.nullToEmpty(issue.title)));
        detailsSection.add(issueTitle, new CC().gapBottom(String.valueOf(UI.scale(4))));

        var issueId = getLabelPanel(RadicleBundle.message("issueId", Strings.nullToEmpty(Utils.formatId(issue.id))));

        final var issueIdAndCopyButton = new NonOpaquePanel(new MigLayout(new LC().insets("0").gridGap("0", "0").noGrid()));
        issueIdAndCopyButton.add(issueId, new CC().gapRight(String.valueOf(JBUIScale.scale(4))));
        issueIdAndCopyButton.add(new Utils.CopyButton(issue.id), new CC().gapRight(String.valueOf(JBUIScale.scale(4))));

        detailsSection.add(issueIdAndCopyButton, new CC().gapBottom(String.valueOf(UI.scale(4))));

        var issueAuthor = getLabelPanel(RadicleBundle.message("issueAuthor", Strings.nullToEmpty(issue.author.generateLabelText())));
        detailsSection.add(issueAuthor, new CC().gapBottom(String.valueOf(UI.scale(4))));

        if (!issue.labels.isEmpty()) {
            var issueTag = getLabelPanel(RadicleBundle.message("issueLabels", String.join(",", issue.labels)));
            detailsSection.add(issueTag, new CC().gapBottom(String.valueOf(UI.scale(4))));
        }

        if (!issue.assignees.isEmpty()) {
            var issueAssignees = getLabelPanel(RadicleBundle.message("issueAssignees",
                    issue.assignees.stream().map(RadAuthor::generateLabelText).collect(Collectors.joining(","))));
            detailsSection.add(issueAssignees, new CC().gapBottom(String.valueOf(UI.scale(4))));
        }

        var issueState = getLabelPanel(RadicleBundle.message("issueState", Strings.nullToEmpty(issue.state.label)));
        detailsSection.add(issueState, new CC().gapBottom(String.valueOf(UI.scale(4))));

        var issueCreated = getLabelPanel(RadicleBundle.message("issueCreated", DATE_TIME_FORMATTER.format(issue.discussion.get(0).timestamp)));
        detailsSection.add(issueCreated, new CC().gapBottom(String.valueOf(UI.scale(4))));

        var borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(detailsSection, BorderLayout.NORTH);
        return borderPanel;
    }

    private JComponent getLabelPanel(String txt) {
        var panel = new NonOpaquePanel();
        panel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        panel.add(new JLabel(txt), new CC().gapBottom(String.valueOf(UI.scale(10))));
        return panel;
    }

    private void addListPanel(JPanel panel, LabeledListPanelHandle<?> handle) {
        panel.add(handle.getTitleLabel(), new CC().alignY("top").width("30"));
        panel.add(handle.getPanel(), new CC().minWidth("0").growX().pushX().wrap());
    }

    protected JComponent createReturnToListSideComponent() {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {
                    issueTabController.createPanel();
                    return Unit.INSTANCE;
                });
    }

    public AssigneesSelect getAssigneesSelect() {
        return assigneesSelect;
    }

    public StateSelect getStateSelect() {
        return stateSelect;
    }

    public LabelSelect getLabelSelect() {
        return labelSelect;
    }

    public class LabelSelect extends LabeledListPanelHandle<LabelSelect.Label> {

        public record Label(String value) { }

        public static class LabelRender extends SelectionListCellRenderer<Label> {

            @Override
            public String getText(Label value) {
                return value.value;
            }

            @Override
            public String getPopupTitle() {
                return RadicleBundle.message("label");
            }
        }

        @Override
        public String getSelectedValues() {
            return String.join(",", issue.labels);
        }

        @Override
        public boolean storeValues(List<Label> labels) {
            var tagList = labels.stream().map(value -> value.value).toList();
            // We don't have changes so don't refresh the window
            if (tagList.size() == issue.labels.size()) {
                return true;
            }
            var resp = api.addRemoveIssueLabel(issue, tagList);
            var isSuccess = resp != null;
            if (isSuccess) {
                issueModel.setValue(issue);
            }
            return isSuccess;
        }

        @Override
        public CompletableFuture<List<Label>> showEditPopup(JComponent parent) {
            var addField = new JBTextField();
            var res = new CompletableFuture<List<Label>>();
            var popUpBuilder = new PopupBuilder();
            jbPopup = popUpBuilder.createPopup(this.getData(), new LabelRender(), false, addField, res);
            jbPopup.showUnderneathOf(parent);
            listener = popUpBuilder.getListener();
            latch = popUpBuilder.getLatch();
            return res.thenApply(data -> {
                if (Strings.isNullOrEmpty(addField.getText())) {
                    return data;
                }
                var myList = new ArrayList<>(data);
                myList.add(new Label(addField.getText()));
                return myList;
            });
        }

        @Override
        public SelectionListCellRenderer<Label> getRender() {
            return new LabelRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<Label>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var labelFuture = new ArrayList<SelectionListCellRenderer.SelectableWrapper<Label>>();
                for (String label : issue.labels) {
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(new Label(label), true);
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

    public class StateSelect extends LabeledListPanelHandle<StateSelect.State> {

        public record State(String status, String label) { }

        public static class StateRender extends SelectionListCellRenderer<StateSelect.State> {

            @Override
            public String getText(StateSelect.State value) {
                return value.label;
            }

            @Override
            public String getPopupTitle() {
                return RadicleBundle.message("state");
            }
        }

        @Override
        public String getSelectedValues() {
            return issue.state.label;
        }

        @Override
        public boolean storeValues(List<StateSelect.State> data) {
            var selectedState = data.get(0).status;
            if (selectedState.equals(issue.state.status)) {
                return true;
            }
            var resp = api.changeIssueState(issue, selectedState);
            var isSuccess = resp != null;
            if (isSuccess) {
                issueModel.setValue(issue);
            }
            return isSuccess;
        }

        @Override
        public SelectionListCellRenderer<State> getRender() {
            return new StateSelect.StateRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<State>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var allStates = Arrays.stream(RadIssue.State.values()).map(e -> new State(e.status, e.label)).toList();
                var stateList = new ArrayList<SelectionListCellRenderer.SelectableWrapper<State>>();
                for (State state : allStates) {
                    var isSelected = issue.state.status.equals(state.status);
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

    public class AssigneesSelect extends LabeledListPanelHandle<AssigneesSelect.Assignee> {

        public record Assignee(String did, String alias) { }

        public static class AssigneeRender extends SelectionListCellRenderer<AssigneesSelect.Assignee> {

            @Override
            public String getText(AssigneesSelect.Assignee value) {
                if (!Strings.isNullOrEmpty(value.alias)) {
                    return value.alias;
                }
                return Utils.formatDid(value.did());
            }

            @Override
            public String getPopupTitle() {
                return RadicleBundle.message("assignees");
            }
        }

        @Override
        public String  getSelectedValues() {
            var formattedDid = new ArrayList<String>();
            for (var as : issue.assignees) {
                if (!Strings.isNullOrEmpty(as.alias)) {
                    formattedDid.add(as.alias);
                } else {
                    formattedDid.add(Utils.formatDid(as.id));
                }
            }
            return String.join(",", formattedDid);
        }

        @Override
        public boolean storeValues(List<AssigneesSelect.Assignee> data) {
            var addAssignees = data.stream().map(Assignee::did).toList();
            // We don't have changes so don't refresh the window
            if (addAssignees.size() == issue.assignees.size()) {
                return true;
            }
            var resp = api.addRemoveIssueAssignees(issue, addAssignees);
            var isSuccess = resp != null;
            if (isSuccess) {
                issueModel.setValue(issue);
            }
            return isSuccess;
        }

        @Override
        public CompletableFuture<List<Assignee>> showEditPopup(JComponent parent) {
            var addField = new JBTextField();
            var res = new CompletableFuture<List<IssuePanel.AssigneesSelect.Assignee>>();
            var popUpBuilder = new PopupBuilder();
            jbPopup = popUpBuilder.createPopup(this.getData(), new IssuePanel.AssigneesSelect.AssigneeRender(), false, addField, res);
            jbPopup.showUnderneathOf(parent);
            listener = popUpBuilder.getListener();
            latch = popUpBuilder.getLatch();
            return res.thenApply(data -> {
                if (Strings.isNullOrEmpty(addField.getText())) {
                    return data;
                }
                var myList = new ArrayList<>(data);
                myList.add(new IssuePanel.AssigneesSelect.Assignee(addField.getText(), null));
                return myList;
            });
        }

        @Override
        public SelectionListCellRenderer<Assignee> getRender() {
            return new AssigneesSelect.AssigneeRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<Assignee>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var projectInfo = api.fetchRadProject(issue.projectId);
                var assignees = new ArrayList<SelectionListCellRenderer.SelectableWrapper<Assignee>>();
                for (var delegate : projectInfo.delegates) {
                    final Assignee assignee = new AssigneesSelect.Assignee(delegate.id, delegate.generateLabelText());
                    final boolean isSelected = issue.assignees.stream().anyMatch(as -> as.id.contains(delegate.id));
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(assignee, isSelected);
                    assignees.add(selectableWrapper);
                }
                for (var assign : issue.assignees) {
                    final boolean exists = assignees.stream().anyMatch(el -> el.value.did.equals(assign.id) ||
                            (el.value.alias != null && el.value.alias.equals(assign.alias)));
                    if (exists) {
                        continue;
                    }
                    var assignee = new AssigneesSelect.Assignee(assign.id, assign.generateLabelText());
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(assignee, true);
                    assignees.add(selectableWrapper);
                }
                return assignees;
            });
        }

        @Override
        public String getLabel() {
            return RadicleBundle.message("assignees");
        }

        @Override
        public boolean isSingleSelection() {
            return false;
        }

    }
}
