package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import kotlin.Unit;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.dialog.PublishDialog;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.LabeledListPanelHandle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.PopupBuilder;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SelectionListCellRenderer;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class CreateIssuePanel {
    private final RadicleProjectApi api;
    private final Project project;
    private AssigneesSelect assigneeSelect;
    private LabelSelect labelSelect;
    public CountDownLatch latch = new CountDownLatch(1);
    private JButton newIssueButton;
    private JBTextArea titleField;
    private DragAndDropField descriptionField;
    protected IssueTabController issueTabController;
    private ComboBox<GitRepository> projectSelect;

    public AssigneesSelect getAssigneeSelect() {
        return assigneeSelect;
    }

    public LabelSelect getLabelSelect() {
        return labelSelect;
    }

    public CreateIssuePanel(IssueTabController issueTabController, Project project) {
        this.issueTabController = issueTabController;
        this.api = project.getService(RadicleProjectApi.class);
        this.project = project;
    }

    public JComponent create() {
        var infoTab = new TabInfo(infoComponent());
        infoTab.setText(RadicleBundle.message("fillFields"));
        infoTab.setSideComponent(createReturnToListSideComponent());
        final var uiDisposable = Disposer.newDisposable(issueTabController.getDisposer(),
                "RadicleIssuePanel");
        SingleHeightTabs tabs = new SingleHeightTabs(null, uiDisposable);
        tabs.addTab(infoTab);
        return tabs;
    }

    private JComponent infoComponent() {
        var mainPanel = new JPanel(new BorderLayout());

        projectSelect = new ComboBox<>();
        projectSelect.setVisible(false);
        projectSelect.setBackground(UIUtil.getListBackground());
        projectSelect.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM), JBUI.Borders.empty(8)));
        projectSelect.setFont(JBFont.label());
        projectSelect.setRenderer(new PublishDialog.ComboBoxCellRenderer());
        /* Remove selected delegates when the user change the project */
        projectSelect.addItemListener(e -> {
            assigneeSelect.delegates = List.of();
            assigneeSelect.refresh();
        });
        CollaborationToolsUIUtil.INSTANCE.registerFocusActions(projectSelect);

        // Find the rad initialized repos and fill the comboBox
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            var repos = gitRepoManager.getRepositories();
            var myRepos = RadAction.getInitializedReposWithNodeConfigured(repos, false);
            latch.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                for (var repo : myRepos) {
                    projectSelect.addItem(repo);
                }
                if (myRepos.size() > 1) {
                    projectSelect.setVisible(true);
                }
            }, ModalityState.any());
        });

        titleField = new JBTextArea(new PlainDocument());
        titleField.setBackground(UIUtil.getListBackground());
        titleField.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM), JBUI.Borders.empty(8)));
        titleField.setFont(JBFont.label());
        titleField.getEmptyText().setText(RadicleBundle.message("myTitle"));
        ((AbstractDocument) titleField.getDocument()).setDocumentFilter(new Utils.RemoveNewLineFilter());
        titleField.setLineWrap(true);
        CollaborationToolsUIUtil.INSTANCE.registerFocusActions(titleField);

        var borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(projectSelect, BorderLayout.NORTH);
        borderPanel.add(titleField, BorderLayout.CENTER);

        descriptionField = new DragAndDropField(project, 0);
        descriptionField.setBackground(UIUtil.getListBackground());
        descriptionField.setFont(JBFont.label());
        descriptionField.getComponent().setBorder(JBUI.Borders.empty(8));
        descriptionField.setPlaceholder(RadicleBundle.message("description"));
        CollaborationToolsUIUtil.INSTANCE.registerFocusActions(descriptionField);

        var textFieldListener = new TextFieldListener();
        titleField.getDocument().addDocumentListener(textFieldListener);
        descriptionField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.@NotNull DocumentEvent event) {
                newIssueButton.setEnabled(!titleField.getText().isEmpty() && !descriptionField.getText().isEmpty());
            }
        });

        var descriptionPane = new JBScrollPane(descriptionField, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionPane.setOpaque(false);
        descriptionPane.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));

        var actionPanelHolder = new JPanel(new BorderLayout());
        actionPanelHolder.add(getActionPanel(), BorderLayout.NORTH);

        var newIssueAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Send api request in order to create a new issue
                var issueTitle = titleField.getText();
                var issueDescription = descriptionField.getText();
                var assignees = assigneeSelect.delegates;
                var labels = labelSelect.storeLabels;
                var repo = (GitRepository) projectSelect.getSelectedItem();
                newIssueButton.setEnabled(false);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    var radInspect = new RadInspect(repo);
                    var output = radInspect.perform();
                    if (!RadAction.isSuccess(output)) {
                        RadAction.showErrorNotification(project,
                                RadicleBundle.message("radCliError"),
                                RadicleBundle.message("errorFindingProjectId"));
                        return;
                    }
                    var radProjectId = output.getStdout().trim();
                    var isSuccess = api.createIssue(issueTitle, issueDescription, assignees, labels, repo, radProjectId, descriptionField.getEmbedList());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (isSuccess) {
                            issueTabController.createPanel();
                        }
                        newIssueButton.setEnabled(true);
                    }, ModalityState.any());
                });
            }
        };

        var cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                issueTabController.createPanel();
            }
        };

        var buttonHolderPanel = new JPanel();
        buttonHolderPanel.setLayout(new MigLayout(new LC().gridGap("0", "0").insets("8").fill().flowY().hideMode(3)));
        buttonHolderPanel.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                JBUI.Borders.empty(8)));
        buttonHolderPanel.add(getButtonsPanel(newIssueAction, cancelAction));

        actionPanelHolder.add(buttonHolderPanel, BorderLayout.CENTER);
        mainPanel.add(borderPanel, BorderLayout.NORTH);
        mainPanel.add(descriptionPane, BorderLayout.CENTER);
        mainPanel.add(actionPanelHolder, BorderLayout.SOUTH);

        return mainPanel;
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            // Enable the new issue button only if the user has fill title & description
            newIssueButton.setEnabled(!titleField.getText().isEmpty() && !descriptionField.getText().isEmpty());
        }
    }

    private JComponent getButtonsPanel(Action newIssueAction, Action cancelAction) {
        newIssueButton = new JButton(newIssueAction);
        newIssueButton.setText(RadicleBundle.message("createIssue"));
        newIssueButton.setEnabled(false);

        var cancelButton = new JButton(cancelAction);
        cancelButton.setText(RadicleBundle.message("cancel"));

        var buttonsPanel = Utils.getHorizontalPanel(8);
        buttonsPanel.add(newIssueButton);
        buttonsPanel.add(cancelButton);
        return buttonsPanel;
    }

    private JPanel getActionPanel() {
        var actionPanel = new JPanel();
        actionPanel.setOpaque(true);
        actionPanel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        actionPanel.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP), JBUI.Borders.empty(8)));

        assigneeSelect = new AssigneesSelect();
        labelSelect = new LabelSelect();
        Utils.addListPanel(actionPanel, assigneeSelect);
        Utils.addListPanel(actionPanel, labelSelect);

        return actionPanel;
    }

    protected JComponent createReturnToListSideComponent() {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {
                    issueTabController.createPanel();
                    return Unit.INSTANCE;
                });
    }

    public class AssigneesSelect extends LabeledListPanelHandle<IssuePanel.AssigneesSelect.Assignee> {
        private List<String> delegates = List.of();

        @Override
        public String getSelectedValues() {
            var formattedDid = new ArrayList<String>();
            for (var delegate : delegates) {
                formattedDid.add(Utils.formatDid(delegate));
            }
            return String.join(",", formattedDid);
        }


        @Override
        public boolean storeValues(List<IssuePanel.AssigneesSelect.Assignee> data) {
            delegates = data.stream().map(IssuePanel.AssigneesSelect.Assignee::name).collect(Collectors.toList());
            refresh();
            return true;
        }

        public void refresh() {
            /* Update the underline model with the new values */
            updateValues();
        }

        @Override
        public SelectionListCellRenderer<IssuePanel.AssigneesSelect.Assignee> getRender() {
            return new IssuePanel.AssigneesSelect.AssigneeRender();
        }

        @Override
        public CompletableFuture<List<IssuePanel.AssigneesSelect.Assignee>> showEditPopup(JComponent parent) {
            var addField = new JBTextField();
            var res = new CompletableFuture<List<IssuePanel.AssigneesSelect.Assignee>>();
            var popUpBuilder = new PopupBuilder();
            jbPopup = popUpBuilder.createPopup(this.getData(), new IssuePanel.AssigneesSelect.AssigneeRender(), false, addField, res);
            jbPopup.showUnderneathOf(parent);
            listener = popUpBuilder.getListener();
            return res.thenApply(data -> {
                if (Strings.isNullOrEmpty(addField.getText())) {
                    return data;
                }
                var myList = new ArrayList<>(data);
                myList.add(new IssuePanel.AssigneesSelect.Assignee(addField.getText()));
                return myList;
            });
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<IssuePanel.AssigneesSelect.Assignee>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var assignees = new ArrayList<SelectionListCellRenderer.SelectableWrapper<IssuePanel.AssigneesSelect.Assignee>>();
                var selectedProject = (GitRepository) projectSelect.getSelectedItem();
                var radInspect = new RadInspect(selectedProject);
                var output = radInspect.perform();
                if (!RadAction.isSuccess(output)) {
                    return assignees;
                }
                var radProjectId = output.getStdout().trim();
                var projectInfo = api.fetchRadProject(radProjectId);
                for (var delegate : projectInfo.delegates) {
                    var assignee = new IssuePanel.AssigneesSelect.Assignee(delegate);
                    var isSelected = delegates.contains(delegate);
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(assignee, isSelected);
                    assignees.add(selectableWrapper);
                }
                for (var delegate : delegates) {
                    var exist = assignees.stream().anyMatch(el -> el.value.name().equals(delegate));
                    if (exist) {
                        continue;
                    }
                    var assignee = new IssuePanel.AssigneesSelect.Assignee(delegate);
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

    public static class LabelSelect extends LabeledListPanelHandle<IssuePanel.LabelSelect.Label> {
        public List<String> storeLabels = new ArrayList<>();

        @Override
        public String getSelectedValues() {
            return String.join(",", storeLabels);
        }

        @Override
        public boolean storeValues(List<IssuePanel.LabelSelect.Label> labels) {
            storeLabels = labels.stream().map(IssuePanel.LabelSelect.Label::value).toList();
            /* Update the underline model with the new values */
            updateValues();
            return true;
        }

        @Override
        public CompletableFuture<List<IssuePanel.LabelSelect.Label>> showEditPopup(JComponent parent) {
            var addField = new JBTextField();
            var res = new CompletableFuture<List<IssuePanel.LabelSelect.Label>>();
            var popUpBuilder = new PopupBuilder();
            jbPopup = popUpBuilder.createPopup(this.getData(), new IssuePanel.LabelSelect.LabelRender(), false, addField, res);
            jbPopup.showUnderneathOf(parent);
            listener = popUpBuilder.getListener();
            return res.thenApply(data -> {
                if (Strings.isNullOrEmpty(addField.getText())) {
                    return data;
                }
                var myList = new ArrayList<>(data);
                myList.add(new IssuePanel.LabelSelect.Label(addField.getText()));
                return myList;
            });
        }

        @Override
        public SelectionListCellRenderer<IssuePanel.LabelSelect.Label> getRender() {
            return new IssuePanel.LabelSelect.LabelRender();
        }

        @Override
        public CompletableFuture<List<SelectionListCellRenderer.SelectableWrapper<IssuePanel.LabelSelect.Label>>> getData() {
            return CompletableFuture.supplyAsync(() -> {
                var labelFuture = new ArrayList<SelectionListCellRenderer.SelectableWrapper<IssuePanel.LabelSelect.Label>>();
                for (String label : storeLabels) {
                    var selectableWrapper = new SelectionListCellRenderer.SelectableWrapper<>(new IssuePanel.LabelSelect.Label(label), true);
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
