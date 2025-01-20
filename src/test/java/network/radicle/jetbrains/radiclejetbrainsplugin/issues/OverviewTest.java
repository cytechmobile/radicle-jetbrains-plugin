package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.execution.CommandLineUtil;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor.IssueEditorProvider;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SelectionListCellRenderer;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssuePanel.DATE_TIME_FORMATTER;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class OverviewTest extends AbstractIT {
    RadicleToolWindow radicleToolWindow;
    RadIssue issue;
    IssueEditorProvider issueEditorProvider;
    VirtualFile editorFile;
    IssueTabController issueTabController;
    Embed txtEmbed;
    Embed imgEmbed;
    String commentDesc;
    String issueDesc;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void beforeTest() throws InterruptedException {
        issue = createIssue();
        setupWindow();
    }

    public void setupWindow() throws InterruptedException {
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        radicleToolWindow = new RadicleToolWindow();
        var mockToolWindow = new PatchListPanelTest.MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), mockToolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(mockToolWindow);
        var contents = radicleToolWindow.getContentManager().getContents();
        radicleToolWindow.getContentManager().setSelectedContent(contents[1]);
        issueTabController = radicleToolWindow.issueTabController;
        if (testName.getMethodName().equals("testReactions")) {
            // Don't recreate IssuePanel after success request for this test
            issueTabController.createInternalIssuePanel(new SingleValueModel<>(issue), new JPanel());
        } else {
            issueTabController.createIssuePanel(issue);
        }
        var editorManager = FileEditorManager.getInstance(getProject());
        var allEditors = editorManager.getAllEditors();
        assertThat(allEditors.length).isEqualTo(1);
        var editor = allEditors[0];
        editorFile = editor.getFile();
        var providerManager = FileEditorProviderManager.getInstance();
        var providers = providerManager.getProviders(getProject(), editorFile);
        assertThat(providers.length).isEqualTo(1);
        issueEditorProvider = (IssueEditorProvider) providers[0];
        // Open createEditor
        issueEditorProvider.createEditor(getProject(), editorFile);
        /* Wait to load the issues */
        Thread.sleep(200);
    }

    @Test
    public void testIssueInfo() {
        var panel = issueTabController.getIssueJPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getFirstComponent();
        var components = actionPanel.getComponents();
        var myPanels = (JPanel) components[0];
        var allPanels = myPanels.getComponents();

        var titleLabel = UIUtil.findComponentOfType((JPanel) allPanels[0], JLabel.class);
        var issueIdLabel = UIUtil.findComponentOfType((JPanel) allPanels[1], JLabel.class);
        var issueAuthorLabel = UIUtil.findComponentOfType((JPanel) allPanels[2], JLabel.class);
        var issueTagLabel = UIUtil.findComponentOfType((JPanel) allPanels[3], JLabel.class);
        var issueAssigneeLabel = UIUtil.findComponentOfType((JPanel) allPanels[4], JLabel.class);
        var issueStateLabel = UIUtil.findComponentOfType((JPanel) allPanels[5], JLabel.class);
        var issueCreatedLabel = UIUtil.findComponentOfType((JPanel) allPanels[6], JLabel.class);

        assertThat(titleLabel.getText()).isEqualTo(RadicleBundle.message("title", Strings.nullToEmpty(issue.title)));
        assertThat(issueIdLabel.getText()).isEqualTo(RadicleBundle.message("issueId", Strings.nullToEmpty(Utils.formatId(issue.id))));
        assertThat(issueAuthorLabel.getText()).isEqualTo(RadicleBundle.message("issueAuthor", issue.author.alias));
        assertThat(issueTagLabel.getText()).isEqualTo(RadicleBundle.message("issueLabels", String.join(",", issue.labels)));
        assertThat(issueAssigneeLabel.getText()).isEqualTo(RadicleBundle.message("issueAssignees",
                String.join(",", issue.assignees.stream().map(as -> as.alias).toList())));
        assertThat(issueStateLabel.getText()).isEqualTo(RadicleBundle.message("issueState", Strings.nullToEmpty(issue.state.label)));
        assertThat(issueCreatedLabel.getText()).isEqualTo(RadicleBundle.message("issueCreated", DATE_TIME_FORMATTER.format(issue.discussion.get(0).timestamp)));
    }

    @Test
    public void addRemoveTags() throws InterruptedException {
        var issuePanel = issueTabController.getIssuePanel();
        var panel = issueTabController.getIssueJPanel();

        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var tagLabel = (JLabel) components[4];
        assertThat(tagLabel.getText()).isEqualTo(RadicleBundle.message("label"));

        var tagPanel = (NonOpaquePanel) components[5];
        var myPanel = (BorderLayoutPanel) tagPanel.getComponent(0);

        // Assert that the label has the selected tags
        var stateValueLabel = (JLabel) myPanel.getComponents()[0];
        assertThat(stateValueLabel.getText()).contains(String.join(",", issue.labels));

        // Find edit key and press it
        var openPopupButton = UIUtil.findComponentOfType(tagPanel, InlineIconButton.class);
        openPopupButton.getActionListener().actionPerformed(new ActionEvent(openPopupButton, 0, ""));
        executeUiTasks();

        var tagSelect = issuePanel.getLabelSelect();
        var popupListener = tagSelect.listener;
        var jblist = UIUtil.findComponentOfType(tagSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jblist.getModel();

        // Trigger beforeShown method
        var fakePopup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup();
        fakePopup.getContent().removeAll();
        fakePopup.getContent().add(new BorderLayoutPanel());

        popupListener.beforeShown(new LightweightWindowEvent(fakePopup));
        //Wait to load tags
        tagSelect.latch.await(5, TimeUnit.SECONDS);
        assertThat(listmodel.getSize()).isEqualTo(2);

        var firstTag = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.LabelSelect.Label>) listmodel.getElementAt(0);
        assertThat(firstTag.value.value()).isEqualTo(issue.labels.get(0));
        assertThat(firstTag.selected).isTrue();

        var secondTag = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.LabelSelect.Label>) listmodel.getElementAt(1);
        assertThat(secondTag.value.value()).isEqualTo(issue.labels.get(1));
        assertThat(secondTag.selected).isTrue();

        clearCommandQueues();
        //Remove first value
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = false;
        popupListener.onClosed(new LightweightWindowEvent(tagSelect.jbPopup));
        // Fix AlreadyDisposedException
        Thread.sleep(1000);
        executeUiTasks();
        var command = radStub.commandsStr.poll(5, TimeUnit.SECONDS);
        assertThat(command).contains("--delete " + CommandLineUtil.posixQuote(firstTag.value.value()));
    }

    @Test
    public void createNewIssueTest() throws InterruptedException {
        var newIssuePanel = new CreateIssuePanel(issueTabController, myProject);
        var tabs = newIssuePanel.create();
        executeUiTasks();
        newIssuePanel.latch.await();

        var panel = (JPanel) tabs.getComponents()[tabs.getComponents().length - 1];
        var children = panel.getComponents();
        var buttonsPanel = (JPanel) ((JPanel) children[2]).getComponents()[1];

        var titleField = UIUtil.findComponentOfType((JComponent) children[0], JBTextArea.class);
        final var issueName = "Test Issue " + UUID.randomUUID();
        titleField.setText(issueName);
        var descriptionField = UIUtil.findComponentOfType((JComponent) children[1], DragAndDropField.class);
        var dummyEmbed = new Embed("98db332aebc4505d7c55e7bfeb9556550220a796", "test.jpg", "data:image/jpeg;base64,test");
        String issueDescription = "Test Description ![" + dummyEmbed.getName() + "](" + dummyEmbed.getOid() + ")";
        descriptionField.setEmbedList(List.of(dummyEmbed));
        descriptionField.setText(issueDescription);

        // Get the panel where the actions select are
        var actionsPanel = (JPanel) ((JPanel) children[2]).getComponents()[0];

        var actionsPanelComponents = actionsPanel.getComponents();
        assertThat(((JLabel) actionsPanelComponents[0]).getText()).isEqualTo(RadicleBundle.message("assignees"));
        assertThat(((JLabel) actionsPanelComponents[2]).getText()).isEqualTo(RadicleBundle.message("label"));

        // Find assignee panel and trigger the open action
        var assigneePanel = (NonOpaquePanel) actionsPanelComponents[1];
        var assigneeButton = UIUtil.findComponentOfType(assigneePanel, InlineIconButton.class);
        assigneeButton.getActionListener().actionPerformed(new ActionEvent(assigneeButton, 0, ""));
        executeUiTasks();

        var assigneeSelect = newIssuePanel.getAssigneeSelect();
        var popUpListener = assigneeSelect.listener;
        var jbList = UIUtil.findComponentOfType(assigneeSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jbList.getModel();

        var fakePopup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup();
        fakePopup.getContent().removeAll();
        fakePopup.getContent().add(new BorderLayoutPanel());
        popUpListener.beforeShown(new LightweightWindowEvent(fakePopup));

        //Wait to load delegates
        var res1 = assigneeSelect.latch.await(10, TimeUnit.SECONDS);
        assertThat(res1).isEqualTo(true);
        assertThat(listmodel.getSize()).isEqualTo(getTestProjects().get(0).delegates.size());

        //Select the first did
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = true;
        popUpListener.onClosed(new LightweightWindowEvent(fakePopup));

        var labelSelect = newIssuePanel.getLabelSelect();
        var label1 = "label1";
        var label2 = "label2";
        labelSelect.storeLabels.add(label1);
        labelSelect.storeLabels.add(label2);

        // Find label panel and trigger the open action
        var labelPanel = (NonOpaquePanel) actionsPanelComponents[3];
        var labelButton = UIUtil.findComponentOfType(labelPanel, InlineIconButton.class);
        labelButton.getActionListener().actionPerformed(new ActionEvent(assigneeButton, 0, ""));
        executeUiTasks();

        var labelPopupListener = labelSelect.listener;
        var labelJbList = UIUtil.findComponentOfType(labelSelect.jbPopup.getContent(), JBList.class);
        var labelListModel = labelJbList.getModel();
        labelPopupListener.beforeShown(new LightweightWindowEvent(fakePopup));

        //Wait to load labels
        labelSelect.latch.await(10, TimeUnit.SECONDS);
        assertThat(labelListModel.getSize()).isEqualTo(2);

        //Remove the first label
        ((SelectionListCellRenderer.SelectableWrapper<?>) labelListModel.getElementAt(0)).selected = false;
        labelPopupListener.onClosed(new LightweightWindowEvent(fakePopup));

        // Find create new issue button
        var createIssueButton = UIUtil.findComponentOfType(buttonsPanel, JButton.class);
        createIssueButton.doClick();
        executeUiTasks();
        var issueCommand = radStub.commandsStr.poll(1, TimeUnit.SECONDS);
        assertThat(issueCommand).isNotNull();
        assertThat(issueCommand).contains(CommandLineUtil.posixQuote(issueName));
        assertThat(issueCommand).contains(CommandLineUtil.posixQuote(issueDescription));
        assertThat(issueCommand).contains(CommandLineUtil.posixQuote(label2));
        assertThat(issueCommand).contains(CommandLineUtil.posixQuote(getTestProjects().getFirst().delegates.getFirst().id));
    }

    @Test
    public void testCopyButton() throws IOException, UnsupportedFlavorException {
        var panel = issueTabController.getIssueJPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var copyButton = UIUtil.findComponentOfType(ef.getFirstComponent(), Utils.CopyButton.class);
        copyButton.doClick();
        var contents = ClipboardSynchronizer.getInstance().getContents();
        var issueId = (String) contents.getTransferData(DataFlavor.stringFlavor);
        assertThat(issueId).isEqualTo(issue.id);
    }

    @Test
    public void changeStateTest() throws InterruptedException {
        var issuePanel = issueTabController.getIssuePanel();
        var panel = issueTabController.getIssueJPanel();

        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var assigneeLabel = (JLabel) components[2];
        assertThat(assigneeLabel.getText()).isEqualTo(RadicleBundle.message("state"));

        var statePanel = (NonOpaquePanel) components[3];
        var myPanel = (BorderLayoutPanel) statePanel.getComponent(0);

        // Assert that the label has the selected state
        var stateValueLabel = (JLabel) myPanel.getComponents()[0];
        assertThat(stateValueLabel.getText()).contains(issue.state.label);

        // Find edit key and press it
        var openPopupButton = UIUtil.findComponentOfType(statePanel, InlineIconButton.class);
        openPopupButton.getActionListener().actionPerformed(new ActionEvent(openPopupButton, 0, ""));
        executeUiTasks();

        var stateSelect = issuePanel.getStateSelect();
        var popupListener = stateSelect.listener;
        var jblist = UIUtil.findComponentOfType(stateSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jblist.getModel();

        // Trigger beforeShown method
        popupListener.beforeShown(new LightweightWindowEvent(JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup()));
        //Wait to load state
        stateSelect.latch.await(5, TimeUnit.SECONDS);
        assertThat(listmodel.getSize()).isEqualTo(2);

        var openState = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.StateSelect.State>) listmodel.getElementAt(0);
        assertThat(openState.value.label()).isEqualTo(issue.state.label);
        assertThat(openState.selected).isTrue();

        var closedState = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.StateSelect.State>) listmodel.getElementAt(1);
        assertThat(closedState.value.label()).isEqualTo(RadIssue.State.CLOSED.label);
        assertThat(closedState.selected).isFalse();

        // Change state to closed
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = false;
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(1)).selected = true;
        clearCommandQueues();
        //Trigger close function in order to trigger the stub and verify the request
        popupListener.onClosed(new LightweightWindowEvent(stateSelect.jbPopup));
        // Fix AlreadyDisposedException
        Thread.sleep(1000);
        executeUiTasks();
        var command = radStub.commands.poll(5, TimeUnit.SECONDS);
        assertThat(command.getCommandLineString()).contains(RadIssue.State.CLOSED.cli);
    }

    @Test
    public void addRemoveAssignersTest() throws InterruptedException {
        var projectDelegates = getTestProjects().getFirst().delegates;
        var issuePanel = issueTabController.getIssuePanel();
        var panel = issueTabController.getIssueJPanel();

        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var assigneeLabel = (JLabel) components[0];
        assertThat(assigneeLabel.getText()).isEqualTo(RadicleBundle.message("assignees"));

        var assigneePanel = (NonOpaquePanel) components[1];
        var myPanel = (BorderLayoutPanel) assigneePanel.getComponent(0);

        // Assert that the label has the selected values
        var assigneeValuesLabel = (JLabel) myPanel.getComponents()[0];
        var assigneeAliases = issue.assignees.stream().map(a -> a.alias).collect(Collectors.joining(","));
        assertThat(assigneeValuesLabel.getText()).contains(assigneeAliases);

        // Find edit key and press it
        var assigneeActionPanel = (NonOpaquePanel) components[1];
        var openPopupButton = UIUtil.findComponentOfType(assigneeActionPanel, InlineIconButton.class);
        openPopupButton.getActionListener().actionPerformed(new ActionEvent(openPopupButton, 0, ""));
        executeUiTasks();

        var assigneesSelect = issuePanel.getAssigneesSelect();
        var popupListener = assigneesSelect.listener;
        var jblist = UIUtil.findComponentOfType(assigneesSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jblist.getModel();

        // Trigger beforeShown method
        var fakePopup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup();
        fakePopup.getContent().removeAll();
        fakePopup.getContent().add(new BorderLayoutPanel());

        popupListener.beforeShown(new LightweightWindowEvent(fakePopup));
        //Wait to load delegates
        assigneesSelect.latch.await(5, TimeUnit.SECONDS);
        assertThat(listmodel.getSize()).as("unexpected number of assignees: expected 3 from the delegates and then another 2 from random issue assignees")
                .isGreaterThanOrEqualTo(3);

        var firstAssignee = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.AssigneesSelect.Assignee>) listmodel.getElementAt(0);
        assertThat(firstAssignee.value.did()).isEqualTo(projectDelegates.get(0).id);
        assertThat(firstAssignee.selected).isTrue();

        var secondAssignee = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.AssigneesSelect.Assignee>) listmodel.getElementAt(1);
        assertThat(secondAssignee.value.did()).isEqualTo(projectDelegates.get(1).id);
        assertThat(secondAssignee.selected).isTrue();

        var thirdAssignee = (SelectionListCellRenderer.SelectableWrapper<IssuePanel.AssigneesSelect.Assignee>) listmodel.getElementAt(2);
        assertThat(thirdAssignee.value.did()).isEqualTo(projectDelegates.get(2).id);
        assertThat(thirdAssignee.selected).isFalse();

        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = false;
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(1)).selected = false;
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(2)).selected = true;

        clearCommandQueues();
        popupListener.onClosed(new LightweightWindowEvent(assigneesSelect.jbPopup));
        // Fix AlreadyDisposedException
        Thread.sleep(1000);
        executeUiTasks();

        var commandStr = radStub.commandsStr.poll(5, TimeUnit.SECONDS);
        assertThat(commandStr).contains("--delete " + CommandLineUtil.posixQuote(firstAssignee.value.did()));
        assertThat(commandStr).contains("--delete " + CommandLineUtil.posixQuote(secondAssignee.value.did()));
        assertThat(commandStr).contains("--add " + CommandLineUtil.posixQuote(thirdAssignee.value.did()));
    }

    @Test
    public void testReactions() throws InterruptedException {
        executeUiTasks();
        var emojiJPanel = issueEditorProvider.getIssueComponent().getEmojiJPanel();
        var emojiLabel = UIUtil.findComponentOfType(emojiJPanel, JLabel.class);
        emojiLabel.getMouseListeners()[0].mouseClicked(null);

        //Check that our reaction exists
        var borderPanel = UIUtil.findComponentOfType(emojiJPanel, BorderLayoutPanel.class);
        var myEmojiLabel = ((JLabel) ((BorderLayoutPanel) ((JPanel) borderPanel.getComponent(1)).getComponent(1)).getComponent(0));
        assertThat(myEmojiLabel.getText()).isEqualTo(issue.discussion.get(0).reactions.get(0).emoji());

        // Make new reaction
        var emojiPanel = issueEditorProvider.getIssueComponent().getEmojiPanel();
        var popUp = emojiPanel.getEmojisPopUp();
        var jblist = UIUtil.findComponentOfType(popUp.getContent(), JBList.class);

        var popUpListener = emojiPanel.getPopupListener();
        popUpListener.beforeShown(new LightweightWindowEvent(JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup()));

        //Wait for the emojis to load
        emojiPanel.getLatch().await(5, TimeUnit.SECONDS);
        var listmodel = jblist.getModel();
        assertThat(listmodel.getSize()).isEqualTo(8);
        //Select the first emoji
        jblist.setSelectedIndex(0);
        jblist.getMouseListeners()[4].mouseClicked(null);
        var selectedEmoji = jblist.getSelectedValue();
        var emoji = (Emoji) ((SelectionListCellRenderer.SelectableWrapper) selectedEmoji).value;
        executeUiTasks();
        Thread.sleep(1000);
        List<GeneralCommandLine> cmds = new ArrayList<>();
        radStub.commands.drainTo(cmds);
        if (cmds.stream().filter(c -> c.getCommandLineString().contains("rad issue react " + issue.id)).findFirst().isEmpty()) {
            var cmd = radStub.commands.poll(5, TimeUnit.SECONDS);
            if (cmd != null) {
                cmds.add(cmd);
            }
        }
        assertThat(cmds).anyMatch(cmd -> cmd.getCommandLineString().contains("rad issue react " + issue.id) &&
                                         cmd.getCommandLineString().contains(issue.discussion.get(1).id));
        // test removing the reaction
        // TODO: jrad: not yet implemented in CLI!
        borderPanel = UIUtil.findComponentOfType(emojiJPanel, BorderLayoutPanel.class);
        var reactorsPanel = ((JPanel) borderPanel.getComponents()[1]).getComponents()[1];
        var listeners = reactorsPanel.getMouseListeners();
        listeners[0].mouseClicked(null);
        executeUiTasks();
        // TODO: verify emoji removed
    }

    @Test
    public void testChangeTitle() throws InterruptedException {
        // TODO: jrad: edit issue title/description is NOT implemented in CLI
        var issueComponent = issueEditorProvider.getIssueComponent();
        var titlePanel = issueComponent.getHeaderPanel();
        var editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();

        var ef = UIUtil.findComponentOfType(titlePanel, DragAndDropField.class);
        /* Test the header title */
        assertThat(ef.getText()).isEqualTo(issue.title);

        UIUtil.markAsShowing((JComponent) ef.getParent(), true);
        //matching UiUtil IS_SHOWING key
        ((JComponent) ef.getParent()).putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(ef.getParent(), false)).isTrue();
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        var editedTitle = "Edited title to " + UUID.randomUUID();
        ef.setText(editedTitle);
        var prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(1);
        /* click the button to edit the patch */
        issue.title = editedTitle;
        prBtn.doClick();
        /* Wait for the reload */
        Thread.sleep(1000);
        var updatedIssueModel = issueTabController.getIssueModel();
        var updatedIssue = updatedIssueModel.getValue();
        assertThat(editedTitle).isEqualTo(updatedIssue.title);
        executeUiTasks();
        // Open createEditor
        issue.repo = firstRepo;
        issue.project = getProject();
        issueEditorProvider.createEditor(getProject(), editorFile);
        issueComponent = issueEditorProvider.getIssueComponent();
        titlePanel = issueComponent.getHeaderPanel();
        executeUiTasks();
        editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();
        ef = UIUtil.findComponentOfType(titlePanel, DragAndDropField.class);
        assertThat(ef.getText()).isEqualTo(editedTitle);

        //Check that notification get triggered
        UIUtil.markAsShowing((JComponent) ef.getParent(), true);
        //matching UiUtil IS_SHOWING key
        ((JComponent) ef.getParent()).putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(ef.getParent(), false)).isTrue();
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();

        notificationsQueue.clear();
        editedTitle = "break";
        ef.setText(editedTitle);
        prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        /* click the button to edit the issue */
        issue.title = editedTitle;
        issue.repo = firstRepo;
        issue.project = getProject();
        prBtn.doClick();
        /* Wait for the reload */
        Thread.sleep(1000);
        executeUiTasks();
        assertThat(ef.getText()).isEqualTo(issue.title);
    }

    @Test
    public void testDescSection() {
        var descSection = issueEditorProvider.getIssueComponent().getDescPanel();
        var elements = UIUtil.findComponentsOfType(descSection, JEditorPane.class);
        var timeline = "";
        for (var el : elements) {
            timeline += el.getText();
        }
        assertThat(timeline).contains(issueDesc);
        assertThat(timeline).contains(getExpectedTag(txtEmbed));
        assertThat(timeline).contains(getExpectedTag(imgEmbed));
    }

    @Test
    public void testReplyComment() {
        executeUiTasks();
        var replyPanel = issueEditorProvider.getIssueComponent().getReplyPanel();
        var replyButton = UIUtil.findComponentsOfType(replyPanel, LinkLabel.class);
        assertThat(replyButton.size()).isEqualTo(1);
        replyButton.get(0).doClick();
        var ef = UIUtil.findComponentOfType(replyPanel, DragAndDropField.class);
        assertThat(ef).isNotNull();
        markAsShowing(ef.getParent(), ef);
        assertThat(ef.getText()).isEmpty();
        executeUiTasks();
        final String replyComment = "This is my reply";
        ef.setText(replyComment);
        var prBtns = UIUtil.findComponentsOfType(replyPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(1);
        prBtn.doClick();
        executeUiTasks();
        var command = radStub.commandsStr.poll();
        assertThat(command).contains("comment");
        assertThat(command).contains(replyComment);
    }

    @Test
    public void testComment() throws InterruptedException {
        // Open createEditor
        issueEditorProvider.createEditor(getProject(), editorFile);
        var issueComponent = issueEditorProvider.getIssueComponent();
        clearCommandQueues();
        executeUiTasks();
        var commentPanel = issueComponent.getCommentFieldPanel();
        var ef = UIUtil.findComponentOfType(commentPanel, DragAndDropField.class);
        UIUtil.markAsShowing((JComponent) ef.getParent(), true);
        //matching UiUtil IS_SHOWING key
        ((JComponent) ef.getParent()).putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(ef.getParent(), false)).isTrue();
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        assertThat(ef.getText()).isEmpty();
        final String dummyComment = "dummy";
        ef.setText(dummyComment);
        var prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(0);
        prBtn.doClick();
        Thread.sleep(1000);
        executeUiTasks();
        var command = radStub.commandsStr.poll();
        assertThat(command).contains("comment");
        assertThat(command).contains(CommandLineUtil.posixQuote(dummyComment));
        assertThat(command).contains(issue.id);
        issue.discussion.add(new RadDiscussion("542", randomAuthor(), dummyComment, Instant.now(), "", List.of(), List.of(), null, null));

        // Open createEditor
        issue.repo = firstRepo;
        issue.project = getProject();
        issueEditorProvider.createEditor(getProject(), editorFile);
        clearCommandQueues();
        executeUiTasks();
        var commentSection = issueEditorProvider.getIssueComponent().getCommentSection();
        var elements = UIUtil.findComponentsOfType(commentSection, JEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(issue.discussion.get(1).author.alias);
        assertThat(comments).contains(commentDesc);
        assertThat(comments).contains(issue.discussion.get(2).author.alias);
        assertThat(comments).contains(issue.discussion.get(2).body);
        assertThat(comments).doesNotContain(issue.discussion.get(0).author.alias);
        assertThat(comments).doesNotContain(issue.discussion.get(0).body);

        //Check that notification get triggered
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        ef.setText("break");
        prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        notificationsQueue.clear();
        prBtn.doClick();
        Thread.sleep(1000);
        executeUiTasks();
        var not = notificationsQueue.poll(20, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("radCliError"));

        // Test edit issue functionality
        // TODO: jrad: Editing issue comment is not available from CLI
        commentPanel = issueComponent.getCommentSection();
        var editBtn = UIUtil.findComponentOfType(commentPanel, InlineIconButton.class);
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        ef = UIUtil.findComponentOfType(commentPanel, DragAndDropField.class);
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        var editedComment = "Edited comment to " + UUID.randomUUID();
        ef.setText(editedComment);
        prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        prBtn.doClick();
        executeUiTasks();

        assertThat(ef.getText()).isEqualTo(editedComment);
    }

    @Test
    public void testCommentsExists() {
        executeUiTasks();
        var commentSection = issueEditorProvider.getIssueComponent().getCommentSection();
        var elements = UIUtil.findComponentsOfType(commentSection, JEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(issue.discussion.get(1).author.alias)
                .contains(commentDesc)
                .contains(getExpectedTag(txtEmbed))
                .contains(getExpectedTag(imgEmbed));
        assertThat(comments).doesNotContain(issue.discussion.getFirst().author.id)
                .doesNotContain(issue.discussion.getFirst().author.alias)
                .doesNotContain(issue.discussion.getFirst().body);
    }

    private String getExpectedTag(Embed dummyEmbed) {
        var expectedUrl = dummyEmbed.getOid();
        if (dummyEmbed.getName().contains(".txt")) {
            return "<a href=\"" + expectedUrl + "\">" + dummyEmbed.getName() + "</a>";
        } else {
            return "<img src=\"" + expectedUrl + "\">";
        }
    }

    private RadIssue createIssue() {
        var txtGitObjectId = UUID.randomUUID().toString();
        var imgGitObjectId = UUID.randomUUID().toString();

        txtEmbed = new Embed(txtGitObjectId, "test.txt", "git:" + txtGitObjectId);
        imgEmbed = new Embed(imgGitObjectId, "test.jpg", "git:" + imgGitObjectId);
        var embeds = List.of(txtEmbed, imgEmbed);

        var txtEmbedMarkDown = "![" + txtEmbed.getName() + "](" + txtEmbed.getOid() + ")";
        var imgEmbedMarkDown = "![" + imgEmbed.getName() + "](" + imgEmbed.getOid() + ")";
        commentDesc = "My Second Comment";
        var commentDescription = commentDesc + txtEmbedMarkDown + imgEmbedMarkDown;

        var discussions = new ArrayList<RadDiscussion>();
        issueDesc = "How are you";
        var myDescription = issueDesc + txtEmbedMarkDown + imgEmbedMarkDown;
        var firstDiscussion = createDiscussion(myDescription, embeds);
        var secondDiscussion = createDiscussion(commentDescription, embeds);
        discussions.add(firstDiscussion);
        discussions.add(secondDiscussion);
        // add first and second delegates of first project as issue assignees (they are expected to be there in assignees test)
        final var prj = getTestProjects().getFirst();
        var myIssue = new RadIssue(UUID.randomUUID().toString(), randomAuthor(), "My Issue",
                RadIssue.State.OPEN, List.of(prj.delegates.getFirst(), prj.delegates.get(1)), List.of("tag1", "tag2"), discussions);
        myIssue.project = getProject();
        myIssue.projectId = UUID.randomUUID().toString();
        myIssue.repo = firstRepo;
        return myIssue;
    }

    private RadDiscussion createDiscussion(String body, List<Embed> embedList) {
        return new RadDiscussion(randomId(), randomAuthor(), body, Instant.now(), "",
                List.of(new Reaction("\uD83D\uDC4D", List.of(randomAuthor()))), embedList, null, null);
    }
}

