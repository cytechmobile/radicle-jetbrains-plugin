package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import git4idea.GitCommit;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Embed;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Emoji;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.Reaction;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchEditorProvider;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DragAndDropField;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SelectionListCellRenderer;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueListPanelTest.getTestIssues;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestPatches;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class TimelineTest extends AbstractIT {
    private static final Logger logger = Logger.getInstance(TimelineTest.class);
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String RAD_PROJECT_ID = "rad:123";

    private String dummyComment = "Hello";
    private RadPatch patch;
    private PatchEditorProvider patchEditorProvider;
    private VirtualFile editorFile;
    private PatchTabController patchTabController;
    private final BlockingQueue<Map<String, Object>> response = new LinkedBlockingQueue<>();
    private static final String PATCH_NAME = "Test Patch";
    private static final String PATCH_DESC = "Test Description";
    private Embed txtEmbed;
    private Embed imgEmbed;
    private String firstComment;
    private String secondComment;
    private String currentRevision = null;
    @Rule
    public TestName testName = new TestName();

    @Before
    public void beforeTest() throws IOException, InterruptedException {
        var api = replaceApiService();
        patch = createPatch();
        currentRevision = firstRepo.getCurrentRevision();
        final var httpClient = api.getClient();
        final int[] statusCode = {200};
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = i.getArgument(0);
            logger.warn("captured request:" + req);
            StringEntity se;
            statusCode[0] = 200;
            if ((req instanceof HttpPut) && ((HttpPut) req).getURI().getPath().contains(SESSIONS_URL)) {
                se = new StringEntity("{}");
            } else if ((req instanceof HttpPatch) && ((HttpPatch) req).getURI().getPath().contains(PATCHES_URL + "/" + patch.id)) {
                var obj = EntityUtils.toString(((HttpPatch) req).getEntity());
                Map<String, Object> map = RadicleProjectApi.MAPPER.readValue(obj, new TypeReference<>() { });
                if (map.get("type").equals("edit")) {
                    //patch
                    assertThat(map.get("target")).isEqualTo("delegates");
                    assertThat(map.get("description")).isEqualTo(patch.description);
                    assertThat(map.get("type")).isEqualTo("edit");
                    assertThat(map.get("title")).isEqualTo(patch.title);
                } else if (map.get("type").equals("label") || map.get("type").equals("lifecycle") || map.get("type").equals("revision.comment.react") ||
                        map.get("type").equals("revision.comment.edit") || map.get("type").equals("revision.comment")) {
                    response.add(map);
                }
                // Return status code 400 in order to trigger the notification
                if (dummyComment.equals("break") || patch.title.equals("break")) {
                    statusCode[0] = 400;
                }
                se = new StringEntity("{}");
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().endsWith(PROJECTS_URL + "/" + RAD_PROJECT_ID)) {
                var map = Map.of("defaultBranch", "main", "id", RAD_PROJECT_ID, "head", currentRevision);
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(map));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains(PATCHES_URL + "/" + patch.id)) {
                var p = new RadPatch(patch);
                p.repo = null;
                p.project = null;
                var map = RadicleProjectApi.MAPPER.convertValue(p, new TypeReference<Map<String, Object>>() { });
                var revisions = (ArrayList<Map<String, Object>>) map.get("revisions");
                for (var rev : revisions) {
                    var discussions = (ArrayList<Map<String, Object>>) rev.get("discussions");
                    for (var discussion : discussions) {
                        var allReactions = new ArrayList<>();
                        var reactions = (ArrayList<Map<String, Object>>) discussion.get("reactions");
                        for (var reaction : reactions) {
                            var reactionList = new ArrayList<>();
                            var nid = reaction.get("nid");
                            var emoji = reaction.get("emoji");
                            reactionList.add(nid);
                            reactionList.add(emoji);
                            allReactions.add(reactionList);
                        }
                        discussion.remove("reactions");
                        discussion.put("reactions", allReactions);
                    }
                }
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(map));
            } else if ((req instanceof HttpPost) && ((HttpPost) req).getURI().getPath().contains(PATCHES_URL)) {
                var obj = EntityUtils.toString(((HttpPost) req).getEntity());
                Map<String, Object> map = RadicleProjectApi.MAPPER.readValue(obj, new TypeReference<>() { });
                response.add(map);
                // We don't need to refresh the panel after the request
                statusCode[0] = 400;
                se = new StringEntity("{}");
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains(PATCHES_URL)) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestPatches()));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().endsWith(ISSUES_URL)) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestIssues()));
            } else if ((req instanceof HttpGet)) {
                // request to fetch specific project
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects().get(0)));
            } else {
                se = new StringEntity("");
            }
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            final var resp = mock(CloseableHttpResponse.class);
            when(resp.getEntity()).thenReturn(se);
            final var statusLine = mock(StatusLine.class);
            when(resp.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(statusCode[0]);
            return resp;
        });
        setupWindow();
    }

    public void setupWindow() throws InterruptedException {
        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        RadicleToolWindow radicleToolWindow = new RadicleToolWindow();
        var mockToolWindow = new PatchListPanelTest.MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), mockToolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(mockToolWindow);
        patchTabController = radicleToolWindow.patchTabController;
        if (testName.getMethodName().equals("testReactions")) {
            // Don't recreate PatchProposalPanel after success request for this test
            patchTabController.createInternalPatchProposalPanel(new SingleValueModel<>(patch), new JPanel());
        } else {
            patchTabController.createPatchProposalPanel(patch);
        }
        var editorManager = FileEditorManager.getInstance(getProject());
        var allEditors = editorManager.getAllEditors();
        assertThat(allEditors.length).isEqualTo(1);
        var editor = allEditors[0];
        editorFile = editor.getFile();
        var providerManager = FileEditorProviderManager.getInstance();
        var providers = providerManager.getProviders(getProject(), editorFile);
        assertThat(providers.length).isEqualTo(1);
        patchEditorProvider = (PatchEditorProvider) providers[0];
        // Open createEditor
        patchEditorProvider.createEditor(getProject(), editorFile);
        /* Wait to load the patches */
        executeUiTasks();
        Thread.sleep(200);
        executeUiTasks();
    }

    @Test
    public void createNewPatchTest() throws InterruptedException {
        var newPatchPanel = new CreatePatchPanel(patchTabController, myProject, List.of(firstRepo)) {
            @Override
            public GitRemote findRadRemote(GitRepository repo, String myUrl) {
                return new GitRemote("test", List.of(), List.of(), List.of(), List.of());
            }
        };
        var tabs = newPatchPanel.create();

        var panel = (JPanel) tabs.getComponents()[6];
        var children = ((JComponent) panel.getComponents()[0]).getComponents();

        panel = (JPanel) children[0];
        children = ((JPanel) panel.getComponents()[0]).getComponents();

        var buttonsPanel = (JPanel) ((JPanel) children[2]).getComponents()[1];
        var titleField = UIUtil.findComponentOfType((JComponent) children[0], JBTextArea.class);
        titleField.setText(PATCH_NAME);
        var descriptionField = UIUtil.findComponentOfType((JComponent) children[1], JBTextArea.class);
        descriptionField.setText(PATCH_DESC);

        // Get the panel where the actions select are
        var actionsPanel = (JPanel) ((JPanel) children[2]).getComponents()[0];
        var actionsPanelComponents = actionsPanel.getComponents();
        assertThat(((JLabel) actionsPanelComponents[0]).getText()).isEqualTo(RadicleBundle.message("label"));

        var labelSelect = newPatchPanel.getLabelSelect();
        var label1 = "label1";
        var label2 = "label2";
        labelSelect.storeLabels.add(label1);
        labelSelect.storeLabels.add(label2);

        // Find label panel and trigger the open action
        var labelPanel = (NonOpaquePanel) actionsPanelComponents[1];
        var labelButton = UIUtil.findComponentOfType(labelPanel, InlineIconButton.class);
        labelButton.getActionListener().actionPerformed(new ActionEvent(labelButton, 0, ""));
        executeUiTasks();

        var labelPopupListener = labelSelect.listener;
        var labelJbList = UIUtil.findComponentOfType(labelSelect.jbPopup.getContent(), JBList.class);
        var labelListModel = labelJbList.getModel();
        var fakePopup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup();
        fakePopup.getContent().removeAll();
        fakePopup.getContent().add(new BorderLayoutPanel());
        labelPopupListener.beforeShown(new LightweightWindowEvent(fakePopup));

        //Wait to load labels
        Thread.sleep(1000);
        executeUiTasks();
        assertThat(labelListModel.getSize()).isEqualTo(2);

        // Find create new patch button
        var createPatchButton = UIUtil.findComponentOfType(buttonsPanel, JButton.class);
        createPatchButton.doClick();
        executeUiTasks();
        var res = response.poll(5, TimeUnit.SECONDS);
        assertThat(res.get("target")).isEqualTo(currentRevision);
        assertThat(res.get("description")).isEqualTo(PATCH_DESC);
        assertThat(res.get("title")).isEqualTo(PATCH_NAME);
        var labels = (ArrayList) res.get("labels");
        assertThat(labels.get(0)).isEqualTo(label1);
        assertThat(labels.get(1)).isEqualTo(label2);
    }

    @Test
    public void testChangeTitle() throws InterruptedException {
        var timelineComponent = patchEditorProvider.getTimelineComponent();
        var titlePanel = timelineComponent.getHeaderPanel();
        var editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();

        var ef = UIUtil.findComponentOfType(titlePanel, DragAndDropField.class);
        /* Test the header title */
        assertThat(ef.getText()).isEqualTo(patch.title);

        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        var editedTitle = "Edited title to " + UUID.randomUUID();
        ef.setText(editedTitle);
        var prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(1);
        /* click the button to edit the patch */
        patch.title = editedTitle;
        prBtn.doClick();
        /* Wait for the reload */
        executeUiTasks();
        Thread.sleep(1000);
        var updatedPatchModel = patchTabController.getPatchModel();
        var updatedPatch = updatedPatchModel.getValue();
        assertThat(editedTitle).isEqualTo(updatedPatch.title);

        // Open createEditor
        patch.repo = firstRepo;
        patch.project = getProject();
        patchEditorProvider.createEditor(getProject(), editorFile);
        timelineComponent = patchEditorProvider.getTimelineComponent();
        titlePanel = timelineComponent.getHeaderPanel();
        editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();
        ef = UIUtil.findComponentOfType(titlePanel, DragAndDropField.class);
        assertThat(ef.getText()).isEqualTo(editedTitle);

        //Check that error notification exists
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();

        notificationsQueue.clear();
        editedTitle = "break";
        ef.setText(editedTitle);
        prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        /* click the button to edit the patch */
        patch.title = editedTitle;
        prBtn.doClick();
        /* Wait for the reload */
        executeUiTasks();
        Thread.sleep(1000);
        executeUiTasks();
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("patchTitleError"));
    }

    @Test
    public void testCheckoutButton() throws InterruptedException {
        var patchProposalPanel = new PatchProposalPanel(patchTabController, new SingleValueModel<>(patch)) {
            @Override
            public void refreshVcs() {

            }
        };
        var panel = patchProposalPanel.createViewPatchProposalPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var myPanel = ef.getFirstComponent();
        var mainPanel = (JPanel) myPanel.getComponents()[0];
        var opaquePanel = (JPanel) mainPanel.getComponents()[2];
        var checkoutButton = (JButton) opaquePanel.getComponents()[2];
        //clear previous commands
        radStub.commands.clear();
        checkoutButton.doClick();
        var checkoutCommand = radStub.commands.poll(10, TimeUnit.SECONDS);
        assertCmd(checkoutCommand);
        assertThat(checkoutCommand.getCommandLineString()).contains("patch checkout " + patch.id.substring(0, 6));
        assertThat(checkoutButton.isEnabled()).isFalse();
    }

    @Test
    public void addRemoveLabels() throws InterruptedException {
        var patchProposalPanel = patchTabController.getPatchProposalPanel();
        var panel = patchTabController.getPatchProposalJPanel();

        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var tagLabel = (JLabel) components[2];
        assertThat(tagLabel.getText()).isEqualTo(RadicleBundle.message("label"));

        var tagPanel = (NonOpaquePanel) components[3];
        var myPanel = (BorderLayoutPanel) tagPanel.getComponent(0);

        // Assert that the label has the selected tags
        var stateValueLabel = (JLabel) myPanel.getComponents()[0];
        assertThat(stateValueLabel.getText()).contains(String.join(",", patch.labels));

        // Find edit key and press it
        var openPopupButton = UIUtil.findComponentOfType(tagPanel, InlineIconButton.class);
        openPopupButton.getActionListener().actionPerformed(new ActionEvent(openPopupButton, 0, ""));
        executeUiTasks();

        var tagSelect = patchProposalPanel.getLabelSelect();
        var popupListener = tagSelect.listener;
        var jblist = UIUtil.findComponentOfType(tagSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jblist.getModel();

        // Trigger beforeShown method
        var fakePopup = JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup();
        fakePopup.getContent().removeAll();
        fakePopup.getContent().add(new BorderLayoutPanel());

        popupListener.beforeShown(new LightweightWindowEvent(fakePopup));
        //Wait to load tags
        executeUiTasks();
        Thread.sleep(1000);
        assertThat(listmodel.getSize()).isEqualTo(2);

        var firstTag = (SelectionListCellRenderer.SelectableWrapper<PatchProposalPanel.LabelSelect.Label>) listmodel.getElementAt(0);
        assertThat(firstTag.value.label()).isEqualTo(patch.labels.get(0));
        assertThat(firstTag.selected).isTrue();

        var secondTag = (SelectionListCellRenderer.SelectableWrapper<PatchProposalPanel.LabelSelect.Label>) listmodel.getElementAt(1);
        assertThat(secondTag.value.label()).isEqualTo(patch.labels.get(1));
        assertThat(secondTag.selected).isTrue();

        //Remove first tag
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = false;
        popupListener.onClosed(new LightweightWindowEvent(tagSelect.jbPopup));

        var res = response.poll(5, TimeUnit.SECONDS);
        var addList = (ArrayList<String>) res.get("labels");
        assertThat(addList.size()).isEqualTo(1);
        assertThat(addList).contains(patch.labels.get(1));
    }

    @Test
    public void testReactions() throws InterruptedException {
        executeUiTasks();
        var emojiJPanel = patchEditorProvider.getTimelineComponent().getComponentsFactory().getEmojiJPanel();
        var emojiLabel = UIUtil.findComponentOfType(emojiJPanel, JLabel.class);
        emojiLabel.getMouseListeners()[0].mouseClicked(null);

        var borderPanel = UIUtil.findComponentOfType(emojiJPanel, BorderLayoutPanel.class);
        var myEmojiLabel = ((JLabel) ((BorderLayoutPanel) ((JPanel) borderPanel.getComponent(1)).getComponent(1)).getComponent(0));
        assertThat(myEmojiLabel.getText()).isEqualTo(patch.revisions.get(0).discussions().get(0).reactions.get(0).emoji);

        // Make new reaction
        var emojiPanel = patchEditorProvider.getTimelineComponent().getComponentsFactory().getEmojiPanel();
        var popUp = emojiPanel.getEmojisPopUp();
        var jblist = UIUtil.findComponentOfType(popUp.getContent(), JBList.class);

        var popUpListener = emojiPanel.getPopupListener();
        popUpListener.beforeShown(new LightweightWindowEvent(JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup()));

        //Wait for the emojis to load
        executeUiTasks();
        Thread.sleep(1000);
        var listmodel = jblist.getModel();
        assertThat(listmodel.getSize()).isEqualTo(8);

        //Select the first emoji
        jblist.setSelectedIndex(0);
        jblist.getMouseListeners()[4].mouseClicked(null);
        var selectedEmoji =  jblist.getSelectedValue();
        var emoji = (Emoji) ((SelectionListCellRenderer.SelectableWrapper) selectedEmoji).value;
        var res = response.poll(5, TimeUnit.SECONDS);
        assertThat(res.get("type")).isEqualTo("revision.comment.react");
        assertThat((Boolean) res.get("active")).isTrue();
        assertThat(res.get("reaction")).isEqualTo(emoji.getUnicode());
        assertThat(res.get("comment")).isEqualTo(patch.revisions.get(patch.revisions.size() - 1).discussions().get(0).id);
        assertThat(res.get("revision")).isEqualTo(patch.revisions.get(patch.revisions.size() - 1).id());

        //Remove reaction
        borderPanel = UIUtil.findComponentOfType(emojiJPanel, BorderLayoutPanel.class);
        var reactorsPanel = ((JPanel) borderPanel.getComponents()[1]).getComponents()[1];
        var listeners = reactorsPanel.getMouseListeners();
        listeners[0].mouseClicked(null);

        res = response.poll(5, TimeUnit.SECONDS);
        assertThat(res.get("type")).isEqualTo("revision.comment.react");
        assertThat((Boolean) res.get("active")).isFalse();
        assertThat(res.get("reaction")).isEqualTo(emoji.getUnicode());
        assertThat(res.get("comment")).isEqualTo(patch.revisions.get(patch.revisions.size() - 1).discussions().get(0).id);
        assertThat(res.get("revision")).isEqualTo(patch.revisions.get(patch.revisions.size() - 1).id());
    }

    @Test
    public void testStateEditButtonWithMergedPatch() {
        patch.state = RadPatch.State.MERGED;
        patchTabController.createPatchProposalPanel(patch);
        var panel = patchTabController.getPatchProposalJPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var statePanel = (NonOpaquePanel) components[1];

        // Assert that if the patch has status merged then the edit button is disable
        var openPopupButton = (InlineIconButton) UIUtil.findComponentOfType(statePanel, InlineIconButton.class);
        assertThat(openPopupButton.isEnabled()).isFalse();
        assertThat(openPopupButton.getTooltip()).isEqualTo(RadicleBundle.message("patchStateChangeTooltip"));
    }

    @Test
    public void changeStateTest() throws InterruptedException {
        var patchProposalPanel = patchTabController.getPatchProposalPanel();
        var panel = patchTabController.getPatchProposalJPanel();

        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getSecondComponent();
        var components = actionPanel.getComponents();
        var stateLabel = (JLabel) components[0];
        assertThat(stateLabel.getText()).isEqualTo(RadicleBundle.message("state"));

        var statePanel = (NonOpaquePanel) components[1];
        var myPanel = (BorderLayoutPanel) statePanel.getComponent(0);

        // Assert that the label has the selected state
        var stateValueLabel = (JLabel) myPanel.getComponents()[0];
        assertThat(stateValueLabel.getText()).contains(patch.state.label);

        // Find edit key and press it
        var openPopupButton = UIUtil.findComponentOfType(statePanel, InlineIconButton.class);
        openPopupButton.getActionListener().actionPerformed(new ActionEvent(openPopupButton, 0, ""));
        executeUiTasks();

        var stateSelect = patchProposalPanel.getStateSelect();
        var popupListener = stateSelect.listener;
        var jblist = UIUtil.findComponentOfType(stateSelect.jbPopup.getContent(), JBList.class);
        var listmodel = jblist.getModel();

        // Trigger beforeShown method
        popupListener.beforeShown(new LightweightWindowEvent(JBPopupFactory.getInstance().createPopupChooserBuilder(new ArrayList<String>()).createPopup()));
        //Wait to load state
        executeUiTasks();
        Thread.sleep(1000);
        assertThat(listmodel.getSize()).isEqualTo(3);

        var openState = (SelectionListCellRenderer.SelectableWrapper<PatchProposalPanel.StateSelect.State>) listmodel.getElementAt(0);
        assertThat(openState.value.label()).isEqualTo(RadPatch.State.OPEN.label);
        assertThat(openState.selected).isTrue();

        var draftState = (SelectionListCellRenderer.SelectableWrapper<PatchProposalPanel.StateSelect.State>) listmodel.getElementAt(1);
        assertThat(draftState.value.label()).isEqualTo(RadPatch.State.DRAFT.label);
        assertThat(draftState.selected).isFalse();

        var archivedState = (SelectionListCellRenderer.SelectableWrapper<PatchProposalPanel.StateSelect.State>) listmodel.getElementAt(2);
        assertThat(archivedState.value.label()).isEqualTo(RadPatch.State.ARCHIVED.label);
        assertThat(archivedState.selected).isFalse();

        // Change state to draft
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(0)).selected = false;
        ((SelectionListCellRenderer.SelectableWrapper<?>) listmodel.getElementAt(1)).selected = true;

        //Trigger close function in order to trigger the stub and verify the request
        popupListener.onClosed(new LightweightWindowEvent(stateSelect.jbPopup));

        var res = response.poll(5, TimeUnit.SECONDS);
        var state = (HashMap<String, String>) res.get("state");
        assertThat(state.get("status")).isEqualTo(RadPatch.State.DRAFT.status);
    }

    @Test
    public void testDescSection() {
        var descSection = patchEditorProvider.getTimelineComponent().getComponentsFactory().getDescSection();
        var elements = UIUtil.findComponentsOfType(descSection, JEditorPane.class);
        var timeline = "";
        for (var el : elements) {
            timeline += el.getText();
        }
        assertThat(timeline).contains(patch.description);
    }

    @Test
    public void testRevSection() {
        executeUiTasks();
        var revisionSection = patchEditorProvider.getTimelineComponent().getRevisionSection();
        executeUiTasks();
        var elements = UIUtil.findComponentsOfType(revisionSection, BaseHtmlEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(patch.revisions.get(0).id());
        assertThat(comments).contains(patch.revisions.get(1).id());
    }

    @Test
    public void testCommentsExists() {
        executeUiTasks();
        var revisionSection = patchEditorProvider.getTimelineComponent().getRevisionSection();
        var elements = UIUtil.findComponentsOfType(revisionSection, JEditorPane.class);
        var comments = elements.stream().map(JEditorPane::getText).collect(Collectors.joining());
        assertThat(comments).contains(firstComment);
        assertThat(comments).contains(patch.revisions.get(0).id());
        assertThat(comments).contains(secondComment);
        assertThat(comments).contains(patch.revisions.get(1).id());
        assertThat(comments).contains(getExpectedTag(txtEmbed));
        assertThat(comments).contains(getExpectedTag(imgEmbed));
    }

    @Test
    public void testEmbeds() throws InterruptedException {
        // Clear previous commands
        radStub.commands.clear();
        executeUiTasks();
        var timelineComponent = patchEditorProvider.getTimelineComponent();
        var commentPanel = timelineComponent.getCommentPanel();
        var ef = UIUtil.findComponentOfType(commentPanel, DragAndDropField.class);
        var dummyEmbed = new Embed("98db332aebc4505d7c55e7bfeb9556550220a796", "test.jpg", "data:image/jpeg;base64,test");
        ef.setEmbedList(List.of(dummyEmbed));
        assertThat(ef).isNotNull();
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        assertThat(ef.getText()).isEmpty();
        dummyComment = dummyComment + "![" + dummyEmbed.getName() + "](" + dummyEmbed.getOid() + ")";
        ef.setText(dummyComment);
        var prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(0);
        prBtn.doClick();
        executeUiTasks();
        Thread.sleep(1000);
        var map = response.poll(5, TimeUnit.SECONDS);
        assertThat(map.get("type")).isEqualTo("revision.comment");
        assertThat((String) map.get("body")).contains(dummyEmbed.getOid());

        var embeds = (ArrayList<HashMap<String, String>>) map.get("embeds");
        assertThat(embeds.get(0).get("oid")).isEqualTo(dummyEmbed.getOid());
        assertThat(embeds.get(0).get("name")).isEqualTo(dummyEmbed.getName());
        assertThat(embeds.get(0).get("content")).isEqualTo(dummyEmbed.getContent());
    }

    @Test
    public void testComment() throws InterruptedException {
        // Clear previous commands
        radStub.commands.clear();
        executeUiTasks();
        var timelineComponent = patchEditorProvider.getTimelineComponent();
        var commentPanel = timelineComponent.getCommentPanel();
        var ef = UIUtil.findComponentOfType(commentPanel, DragAndDropField.class);
        assertThat(ef).isNotNull();
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        assertThat(ef.getText()).isEmpty();
        ef.setText(dummyComment);
        var prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(0);
        prBtn.doClick();
        executeUiTasks();
        Thread.sleep(1000);
        //Comment
        var map = response.poll(5, TimeUnit.SECONDS);
        assertThat(map.get("revision")).isEqualTo(patch.revisions.get(patch.revisions.size() - 1).id());
        assertThat(map.get("body")).isEqualTo(dummyComment);
        var discussion = new RadDiscussion("542", new RadAuthor("myTestAuthor"), dummyComment, Instant.now(), "", List.of(), List.of());
        patch.revisions.get(patch.revisions.size() - 1).discussions().add(discussion);

        // Open createEditor
        patch.repo = firstRepo;
        patch.project = getProject();
        patchEditorProvider.createEditor(getProject(), editorFile);
        radStub.commands.clear();
        executeUiTasks();
        Thread.sleep(1000);

        var revisionSection = patchEditorProvider.getTimelineComponent().getRevisionSection();
        executeUiTasks();
        Thread.sleep(1000);

        var elements = UIUtil.findComponentsOfType(revisionSection, JEditorPane.class);
        assertThat(elements).isNotEmpty();
        var comments = elements.stream().map(JEditorPane::getText).collect(Collectors.joining());
        assertThat(comments).contains(firstComment);
        assertThat(comments).contains(patch.revisions.get(0).id());
        assertThat(comments).contains(secondComment);
        assertThat(comments).contains(patch.revisions.get(1).id());
        // Assert that the new comment exists
        assertThat(comments).contains(patch.revisions.get(1).discussions().get(1).body);

        //Check that notification get triggered
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        dummyComment = "break";
        ef.setText(dummyComment);
        prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        prBtn.doClick();
        executeUiTasks();
        Thread.sleep(1000);
        response.poll(5, TimeUnit.SECONDS);
        executeUiTasks();
        var not = notificationsQueue.poll(20, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("commentError"));

        // Test edit patch functionality
        var commPanel = timelineComponent.getComponentsFactory().getCommentPanel();
        var editBtn = UIUtil.findComponentOfType(commPanel, InlineIconButton.class);
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        ef = UIUtil.findComponentOfType(commPanel, DragAndDropField.class);
        markAsShowing(ef.getParent(), ef);
        executeUiTasks();
        var editedComment = "Edited comment to " + UUID.randomUUID();
        ef.setText(editedComment);
        prBtns = UIUtil.findComponentsOfType(commPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        prBtn.doClick();

        var res = response.poll(5, TimeUnit.SECONDS);
        assertThat(res.get("type")).isEqualTo("revision.comment.edit");
        assertThat(res.get("revision")).isEqualTo(patch.revisions.get(1).id());
        assertThat(res.get("body")).isEqualTo(editedComment);
        assertThat(res.get("comment")).isEqualTo(patch.revisions.get(1).discussions().get(0).id);
    }

    private RadPatch createPatch() {
        var txtGitObjectId = UUID.randomUUID().toString();
        var imgGitObjectId = UUID.randomUUID().toString();

        txtEmbed = new Embed(txtGitObjectId, "test.txt", "git:" + txtGitObjectId);
        imgEmbed = new Embed(imgGitObjectId, "test.jpg", "git:" + imgGitObjectId);
        var txtEmbedMarkDown = "![" + txtEmbed.getName() + "](" + txtEmbed.getOid() + ")";
        var imgEmbedMarkDown = "![" + imgEmbed.getName() + "](" + imgEmbed.getOid() + ")";
        firstComment = "hello";
        secondComment = "hello back";
        var firstCommit = commitHistory.get(0);
        var secondCommit = commitHistory.get(1);
        var firstDiscussion = createDiscussion("123", "123", firstComment + txtEmbedMarkDown + imgEmbedMarkDown, List.of(txtEmbed, imgEmbed));
        var secondDiscussion = createDiscussion("321", "321", secondComment + txtEmbedMarkDown + imgEmbedMarkDown, List.of(txtEmbed, imgEmbed));
        var firstRev = createRevision("testRevision1", "testRevision1", firstCommit, firstDiscussion);
        var secondRev = createRevision("testRevision2", "testRevision1", secondCommit, secondDiscussion);
        var myPatch = new RadPatch("c5df12", RAD_PROJECT_ID, "testPatch", new RadAuthor(AUTHOR), "testDesc",
                "testTarget", List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(firstRev, secondRev));
        myPatch.project = getProject();
        myPatch.repo = firstRepo;
        myPatch.projectId = UUID.randomUUID().toString();
        return myPatch;
    }
    private String getExpectedTag(Embed dummyEmbed) {
        var expectedUrl = radicleProjectSettingsHandler.loadSettings().getSeedNode().url + "/raw/" + patch.projectId + "/blobs/" + dummyEmbed.getOid();
        if (dummyEmbed.getName().contains(".txt")) {
            return "<a href=\"" + expectedUrl + "?mime=text/plain\">" + dummyEmbed.getName() + "</a>";
        } else {
            return "<img src=\"" + expectedUrl + "?mime=image/jpeg\">";
        }
    }

    private RadPatch.Revision createRevision(String id, String description, GitCommit commit, RadDiscussion discussion) {
        var firstChange = commit.getChanges().stream().findFirst().orElseThrow();
        var base = firstChange.getBeforeRevision().getRevisionNumber().asString();
        var discussions = new ArrayList<RadDiscussion>();
        discussions.add(discussion);
        return new RadPatch.Revision(id, description, base, commit.getId().asString(),
                List.of("branch"), List.of(), Instant.now(), discussions, List.of());
    }

    private RadDiscussion createDiscussion(String id, String authorId, String body, List<Embed> embedList) {
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of(new Reaction("fakeDid", "\uD83D\uDC4D")), embedList);
    }
}
