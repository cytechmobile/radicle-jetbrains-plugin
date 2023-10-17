package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.ReturnToListComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.SingleHeightTabs;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.ui.frame.ProgressStripe;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import kotlin.Unit;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.CreateIssuePanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectService;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.PopupBuilder;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ScrollPaneConstants;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH;

public class CreatePatchPanel {
    private static final Logger logger = LoggerFactory.getLogger(CreatePatchPanel.class);
    private final ComboBox<GitRepository> selectedRepo = new ComboBox<>();
    private final ComboBox<GitLocalBranch> selectedBranch = new ComboBox<>();
    private final PatchComponentFactory patchComponentFactory;
    private final PatchTabController patchTabController;
    private final RadicleProjectService radicleProjectService;
    private final List<GitRepository> repos;
    private final RadicleProjectApi api;
    private final Project project;

    private CreateIssuePanel.LabelSelect labelSelect;
    private ProgressStripe progressStripe;
    private Map<String, Object> projectInfo;
    private JBTextArea titleField;
    private JBTextArea descriptionField;
    private JPanel selectRepoPanel;
    private JButton newPatchButton;
    private JPanel loadingPanel;

    public CreatePatchPanel(PatchTabController patchTabController, Project project, List<GitRepository> repos) {
        this.patchComponentFactory = new PatchComponentFactory(project, patchTabController.getDisposer());
        this.radicleProjectService = project.getService(RadicleProjectService.class);
        this.patchTabController = patchTabController;
        this.project = project;
        this.repos = repos;
        this.api = this.project.getService(RadicleProjectApi.class);
    }

    public JComponent create() {
        final var uiDisposable = Disposer.newDisposable(patchTabController.getDisposer(), "RadiclePatchCreate");
        var tabs = new SingleHeightTabs(null, uiDisposable);

        var infoComponent = infoComponent();
        var infoTab = new TabInfo(infoComponent);
        infoTab.setText(RadicleBundle.message("info"));
        infoTab.setSideComponent(createReturnToListSideComponent());

        var filesComponent = patchComponentFactory.createFilesComponent();
        var fileTabInfo = new TabInfo(filesComponent);
        fileTabInfo.setText(RadicleBundle.message("files"));
        fileTabInfo.setSideComponent(createReturnToListSideComponent());
        patchComponentFactory.setFileTab(fileTabInfo);

        var commitComponent = patchComponentFactory.createCommitComponent();
        var commitTabInfo = new TabInfo(commitComponent);
        commitTabInfo.setText(RadicleBundle.message("commits"));
        commitTabInfo.setSideComponent(createReturnToListSideComponent());
        patchComponentFactory.setCommitTab(commitTabInfo);

        tabs.addTab(infoTab);
        tabs.addTab(fileTabInfo);
        tabs.addTab(commitTabInfo);

        return tabs;
    }

    private void updateCommitFileComponents(LinkLabel<?> fromLabel, JLabel toLabel) {
        var mySelectedBranch = (GitLocalBranch) this.selectedBranch.getSelectedItem();
        var mySelectedRepo = ((GitRepository) this.selectedRepo.getSelectedItem());
        if (mySelectedRepo == null || mySelectedBranch == null) {
            logger.warn("No selected values found");
            return;
        }
        fromLabel.setText(mySelectedRepo.getRoot().getName() + ":" + mySelectedBranch.getName());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            //Start loading indicator
            progressStripe.startLoading();
            //Get project information from httpd
            projectInfo = getProjectInfo(mySelectedRepo);
            if (projectInfo == null) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                //Update the file & commit panel
                patchComponentFactory.updateFileComponent(mySelectedRepo, mySelectedBranch, (String) projectInfo.get("head"));
                patchComponentFactory.updateCommitComponent(mySelectedRepo, mySelectedBranch, (String) projectInfo.get("head"));
                toLabel.setText(mySelectedRepo.getRoot().getName() + ":" + projectInfo.get("defaultBranch"));
                //Show panel after everything has been initialized
                selectRepoPanel.setVisible(true);
                progressStripe.stopLoading();
            }, ModalityState.any());
        });
    }

    private void showPopUp(LinkLabel<?> fromLabel, JLabel toLabel) {
        var gridLayout = new JPanel(new GridLayoutManager(3, 2));
        gridLayout.setPreferredSize(new Dimension(400, 100));
        var constraints = new GridConstraints();
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        constraints.setIndent(1);
        constraints.setRow(0);
        constraints.setColumn(0);

        gridLayout.add(new JLabel(RadicleBundle.message("headRepo")), constraints);
        constraints.setColumn(1);
        constraints.setFill(FILL_BOTH);
        gridLayout.add(selectedRepo, constraints);

        constraints.setRow(1);
        constraints.setColumn(0);
        constraints.setIndent(1);
        gridLayout.add(new JLabel(RadicleBundle.message("branch")), constraints);
        constraints.setColumn(1);
        constraints.setFill(FILL_BOTH);
        gridLayout.add(selectedBranch, constraints);

        var saveButton = new JButton(RadicleBundle.message("save"));
        constraints.setRow(2);
        constraints.setColumn(1);
        gridLayout.add(saveButton, constraints);

        initCombobox();

        var popUp = new PopupBuilder().createHorizontalPopup(gridLayout, false, false);
        popUp.showUnderneathOf(fromLabel);

        saveButton.addActionListener(e -> {
            popUp.closeOk(null);
            updateCommitFileComponents(fromLabel, toLabel);
        });
    }

    private JComponent infoComponent() {
        var mainPanel = new JPanel(new BorderLayout());
        progressStripe = new ProgressStripe(mainPanel, patchTabController.getDisposer(), ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
        selectedRepo.setRenderer(new GitRepoCellRenderer());
        selectedRepo.addActionListener(new ComboBoxSelectionListener());
        selectedBranch.setRenderer(new LocalBranchRenderer());

        var toLabel = new JLabel();
        var fromLabel = Utils.createLinkLabel("", (aSource, aLinkData) -> showPopUp(aSource, toLabel));

        //Initialize combobox and calculate commits and file changes
        initCombobox();
        updateCommitFileComponents(fromLabel, toLabel);

        selectRepoPanel = new JPanel();
        selectRepoPanel.setLayout(new BoxLayout(selectRepoPanel, BoxLayout.X_AXIS));
        selectRepoPanel.add(toLabel);
        selectRepoPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        // Show the panel after initialization
        selectRepoPanel.setVisible(false);

        var arrowLabel = new JLabel("<-");
        arrowLabel.setForeground(JBColor.GRAY);
        selectRepoPanel.add(arrowLabel);
        selectRepoPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        selectRepoPanel.add(fromLabel);
        selectRepoPanel.setBackground(UIUtil.getListBackground());
        selectRepoPanel.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM), JBUI.Borders.empty(8)));

        var textFieldListener = new TextFieldListener();
        titleField = new JBTextArea(new PlainDocument());
        titleField.getDocument().addDocumentListener(textFieldListener);
        titleField.setBackground(UIUtil.getListBackground());
        titleField.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM), JBUI.Borders.empty(8)));
        titleField.setFont(JBFont.label());
        titleField.getEmptyText().setText(RadicleBundle.message("myTitle"));
        titleField.setLineWrap(true);
        CollaborationToolsUIUtil.INSTANCE.registerFocusActions(titleField);

        var borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(selectRepoPanel, BorderLayout.NORTH);
        borderPanel.add(titleField, BorderLayout.CENTER);

        descriptionField = new JBTextArea(new PlainDocument());
        descriptionField.getDocument().addDocumentListener(textFieldListener);
        descriptionField.setBackground(UIUtil.getListBackground());
        descriptionField.setBorder(JBUI.Borders.empty(8, 8, 0, 8));
        descriptionField.setFont(JBFont.label());
        descriptionField.getEmptyText().setText(RadicleBundle.message("description"));
        descriptionField.setLineWrap(true);
        CollaborationToolsUIUtil.INSTANCE.registerFocusActions(descriptionField);

        var descriptionPane = new JBScrollPane(descriptionField, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionPane.setOpaque(false);
        descriptionPane.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
        //Create new patch action
        var newPatchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                var mySelectedBranch = (GitLocalBranch) selectedBranch.getSelectedItem();
                var mySelectedRepo = (GitRepository) selectedRepo.getSelectedItem();
                if (mySelectedRepo == null || mySelectedBranch == null) {
                    logger.warn("No selected values found");
                    return;
                }
                var projectId = (String) projectInfo.get("id");
                var remote = findRadRemote(mySelectedRepo, projectId.split(":")[1]);
                if (remote == null) {
                    logger.warn("Unable to find rad remote. Project ID : {}", projectId);
                    RadAction.showErrorNotification(project, RadicleBundle.message("findRemoteError"),
                            RadicleBundle.message("findRemoteErrorDesc"));
                    return;
                }
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    newPatchButton.setEnabled(false);
                    loadingPanel.setVisible(true);
                    var gitPushRepoResult = radicleProjectService.pushChanges(mySelectedRepo, mySelectedBranch, remote);
                    if (gitPushRepoResult == null) {
                        RadAction.showErrorNotification(project, RadicleBundle.message("gitPushError"), "");
                        loadingPanel.setVisible(false);
                        newPatchButton.setEnabled(true);
                        return;
                    }
                    var isSuccess = radicleProjectService.isSuccessPush(gitPushRepoResult);
                    if (!isSuccess) {
                        var errorMsg = gitPushRepoResult.getError();
                        RadAction.showErrorNotification(project, RadicleBundle.message("gitPushError"), errorMsg);
                        loadingPanel.setVisible(false);
                        newPatchButton.setEnabled(true);
                        return;
                    }
                    var branchRevision = radicleProjectService.getBranchRevision(project, mySelectedRepo, mySelectedBranch.getName());
                    if (Strings.isNullOrEmpty(branchRevision)) {
                        RadAction.showErrorNotification(project,
                                RadicleBundle.message("resolveRevNumberError"),
                                RadicleBundle.message("resolveRevNumberErrorDesc"));
                        loadingPanel.setVisible(false);
                        newPatchButton.setEnabled(true);
                        return;
                    }
                    var success = api.createPatch(titleField.getText(), descriptionField.getText(), labelSelect.storeLabels, (String) projectInfo.get("head"),
                            branchRevision, mySelectedRepo, (String) projectInfo.get("id"));
                    if (success) {
                        //Fetch the patch that created from the remote
                        radicleProjectService.fetchPeerChanges(mySelectedRepo);
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success) {
                            patchTabController.createPanel();
                        }
                        newPatchButton.setEnabled(true);
                        loadingPanel.setVisible(false);
                    }, ModalityState.any());
                });
            }
        };

        var cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                patchTabController.createPanel();
            }
        };
        var buttonHolderPanel = new JPanel();
        buttonHolderPanel.setLayout(new MigLayout(new LC().gridGap("0", "7").insets("8").fill().flowY().hideMode(3)));
        buttonHolderPanel.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                JBUI.Borders.empty(8)));

        loadingPanel = new JPanel();
        loadingPanel.setVisible(false);
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.X_AXIS));
        var spinnerIcon = new AsyncProcessIcon(RadicleBundle.message("creatingPatch"));
        loadingPanel.add(spinnerIcon);
        loadingPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        var loadingLabel = new JLabel(RadicleBundle.message("creatingPatch"));
        loadingPanel.add(loadingLabel);
        buttonHolderPanel.add(loadingPanel);
        buttonHolderPanel.add(getButtonsPanel(newPatchAction, cancelAction));

        var actionPanelHolder = new JPanel(new BorderLayout());
        actionPanelHolder.add(getActionPanel(), BorderLayout.NORTH);
        actionPanelHolder.add(buttonHolderPanel, BorderLayout.CENTER);

        mainPanel.add(borderPanel, BorderLayout.NORTH);
        mainPanel.add(descriptionPane, BorderLayout.CENTER);
        mainPanel.add(actionPanelHolder, BorderLayout.SOUTH);

        return progressStripe;
    }

    protected GitRemote findRadRemote(GitRepository repo, String myUrl) {
        var remotes = repo.getRemotes();
        for (var remote : remotes) {
            for (var url : remote.getUrls()) {
                if (url.contains(myUrl)) {
                    return remote;
                }
            }
        }
        return null;
    }

    private JComponent getButtonsPanel(Action newIssueAction, Action cancelAction) {
        newPatchButton = new JButton(newIssueAction);
        newPatchButton.setText(RadicleBundle.message("createPatch"));
        newPatchButton.setEnabled(false);

        var cancelButton = new JButton(cancelAction);
        cancelButton.setText(RadicleBundle.message("cancel"));

        var buttonsPanel = Utils.getHorizontalPanel(8);
        buttonsPanel.add(newPatchButton);
        buttonsPanel.add(cancelButton);
        return buttonsPanel;
    }

    private JPanel getActionPanel() {
        var actionPanel = new JPanel();
        actionPanel.setOpaque(true);
        actionPanel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));
        actionPanel.setBorder(BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP), JBUI.Borders.empty(8)));
        labelSelect = new CreateIssuePanel.LabelSelect();
        Utils.addListPanel(actionPanel, labelSelect);
        return actionPanel;
    }

    private void initCombobox() {
        //Init combobox with repos
        if (selectedRepo.getModel().getSize() == 0) {
            for (var repo : repos) {
                selectedRepo.addItem(repo);
            }
        }
        //Show only the branches that belong to the specific selected repo without showing patches
        selectedBranch.removeAllItems();
        var branches = ((GitRepository) Objects.requireNonNull(selectedRepo.getSelectedItem())).getBranches().getLocalBranches();
        for (var branch : branches) {
            if (!branch.getName().startsWith("patches")) {
                selectedBranch.addItem(branch);
            }
        }
    }

    private class ComboBoxSelectionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            initCombobox();
        }
    }

    public class TextFieldListener extends DocumentAdapter {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
            // Enable the patch button only if the user has fill title & description
            newPatchButton.setEnabled(!titleField.getText().isEmpty() && !descriptionField.getText().isEmpty());
        }
    }

    private Map<String, Object> getProjectInfo(GitRepository repo) {
        var radInspect = new RadInspect(repo);
        var output = radInspect.perform();
        if (!RadAction.isSuccess(output)) {
            return null;
        }
        var projectId = output.getStdout().trim();
        return api.getProjectInfo(projectId, repo);
    }

    protected JComponent createReturnToListSideComponent() {
        return ReturnToListComponent.INSTANCE.createReturnToListSideComponent(RadicleBundle.message("backToList"),
                () -> {
                    patchTabController.createPanel();
                    return Unit.INSTANCE;
                });
    }

    private static class LocalBranchRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof GitLocalBranch branch) {
                setText(branch.getName());
            }
            return this;
        }
    }

    private static class GitRepoCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof GitRepository repo) {
                setText(repo.getRoot().getName());
            }
            return this;
        }
    }

    public CreateIssuePanel.LabelSelect getLabelSelect() {
        return labelSelect;
    }
}
