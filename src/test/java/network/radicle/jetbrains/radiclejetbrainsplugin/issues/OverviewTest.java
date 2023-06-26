package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.UIUtil;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.overview.editor.IssueEditorProvider;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import javax.swing.JComponent;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueListPanelTest.getTestIssues;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestPatches;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class OverviewTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private static String dummyComment = "Hello";
    private RadIssue issue;
    private IssueEditorProvider issueEditorProvider;
    private VirtualFile editorFile;
    private IssueTabController issueTabController;

    @Before
    public void beforeTest() throws IOException, InterruptedException {
        var api = replaceApiService();
        issue = createIssue();
        final var httpClient = api.getClient();
        final int[] statusCode = {200};
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = i.getArgument(0);
            StringEntity se;
            statusCode[0] = 200;
            if ((req instanceof HttpPut) && ((HttpPut) req).getURI().getPath().contains(SESSIONS_URL)) {
                se = new StringEntity("{}");
            }  else if ((req instanceof HttpPatch) && ((HttpPatch) req).getURI().getPath().contains(ISSUES_URL + "/" + issue.id)) {
                var obj = EntityUtils.toString(((HttpPatch) req).getEntity());
                var mapper = new ObjectMapper();
                Map<String, Object> map = mapper.readValue(obj, Map.class);
                var action = (HashMap<String, String>) map.get("action");
                //Assert that we send the correct payload to the api
                if (map.get("type").equals("thread")) {
                    //Comment
                    assertThat(map.get("type")).isEqualTo("thread");
                    assertThat(action.get("type")).isEqualTo("comment");
                    assertThat(action.get("body")).isEqualTo(dummyComment);
                    issue.discussion.add(new RadDiscussion("542", new RadAuthor("das"), dummyComment, Instant.now(), "", List.of()));
                } else if (map.get("type").equals("edit")) {
                    //Issue
                    assertThat(map.get("type")).isEqualTo("edit");
                    assertThat(map.get("title")).isEqualTo(issue.title);
                }
                // Return status code 400 in order to trigger the notification
                if (dummyComment.equals("break") || issue.title.equals("break")) {
                    statusCode[0] = 400;
                }
                se = new StringEntity("{}");
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains(ISSUES_URL + "/" + issue.id)) {
                issue.repo = null;
                issue.project = null;
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(issue));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains("/patches")) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestPatches()));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().endsWith(ISSUES_URL)) {
                // request to fetch issues
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
        issueTabController = radicleToolWindow.issueTabController;
        issueTabController.createIssuePanel(issue);
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
    public void testChangeTitle() throws InterruptedException {
        var issueComponent = issueEditorProvider.getIssueComponent();
        var titlePanel = issueComponent.getHeaderPanel();
        var editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();

        var ef = UIUtil.findComponentOfType(titlePanel, EditorTextField.class);
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
        issueComponent.getLatch().await();
        var updatedIssueModel = issueTabController.getIssueModel();
        var updatedIssue = updatedIssueModel.getValue();
        assertThat(editedTitle).isEqualTo(updatedIssue.title);

        // Open createEditor
        issue.repo = firstRepo;
        issue.project = getProject();
        issueEditorProvider.createEditor(getProject(), editorFile);
        issueComponent = issueEditorProvider.getIssueComponent();
        titlePanel = issueComponent.getHeaderPanel();
        editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();
        ef = UIUtil.findComponentOfType(titlePanel, EditorTextField.class);
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
        /* click the button to edit the patch */
        issue.title = editedTitle;
        prBtn.doClick();
        /* Wait for the reload */
        issueComponent.getLatch().await();
        executeUiTasks();
        var not = notificationsQueue.poll(10, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("issueTitleError"));
    }

    @Test
    public void testDescSection() {
        var descSection = issueEditorProvider.getIssueComponent().getDescPanel();
        var elements = UIUtil.findComponentsOfType(descSection, BaseHtmlEditorPane.class);
        var timeline = "";
        for (var el : elements) {
            timeline += el.getText();
        }
        assertThat(timeline).contains(issue.discussion.get(0).body);
    }

    @Test
    public void testComment() throws InterruptedException {
        // Open createEditor
        issueEditorProvider.createEditor(getProject(), editorFile);
        var issueComponent = issueEditorProvider.getIssueComponent();
        radStub.commands.clear();
        executeUiTasks();
        var commentPanel = issueComponent.getCommentFieldPanel();
        var ef = UIUtil.findComponentOfType(commentPanel, EditorTextField.class);
        UIUtil.markAsShowing((JComponent) ef.getParent(), true);
        //matching UiUtil IS_SHOWING key
        ((JComponent) ef.getParent()).putClientProperty(Key.findKeyByName("Component.isShowing"), Boolean.TRUE);
        assertThat(UIUtil.isShowing(ef.getParent(), false)).isTrue();
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        assertThat(ef.getText()).isEmpty();
        ef.setText(dummyComment);
        var prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        var prBtn = prBtns.get(1);
        prBtn.doClick();
        issueComponent.getLatch().await();

        // Open createEditor
        issue.repo = firstRepo;
        issue.project = getProject();
        issueEditorProvider.createEditor(getProject(), editorFile);
        radStub.commands.clear();
        executeUiTasks();
        var commentSection = issueEditorProvider.getIssueComponent().getCommentSection();
        var elements = UIUtil.findComponentsOfType(commentSection, BaseHtmlEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(issue.discussion.get(1).author.id);
        assertThat(comments).contains(issue.discussion.get(1).body);
        assertThat(comments).contains(issue.discussion.get(2).author.id);
        assertThat(comments).contains(issue.discussion.get(2).body);
        assertThat(comments).doesNotContain(issue.discussion.get(0).author.id);
        assertThat(comments).doesNotContain(issue.discussion.get(0).body);

        //Check that notification get triggered
        markAsShowing(ef.getParent());
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        dummyComment = "break";
        dummyComment = "break";
        ef.setText(dummyComment);
        prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        prBtn = prBtns.get(1);
        prBtn.doClick();
        Thread.sleep(1000);
        issueComponent.getLatch().await();
        executeUiTasks();
        var not = notificationsQueue.poll(20, TimeUnit.SECONDS);
        assertThat(not).isNotNull();
        assertThat(not.getTitle()).isEqualTo(RadicleBundle.message("commentError"));
    }

    @Test
    public void testCommentsExists() {
        executeUiTasks();
        var commentSection = issueEditorProvider.getIssueComponent().getCommentSection();
        var elements = UIUtil.findComponentsOfType(commentSection, BaseHtmlEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(issue.discussion.get(1).author.id);
        assertThat(comments).contains(issue.discussion.get(1).body);
        assertThat(comments).doesNotContain(issue.discussion.get(0).author.id);
        assertThat(comments).doesNotContain(issue.discussion.get(0).body);
    }

    private RadIssue createIssue() {
        var discussions = new ArrayList<RadDiscussion>();
        var firstDiscussion = createDiscussion("123", "123", "How are you");
        var secondDiscussion = createDiscussion("321", "321", "My Second Comment");
        discussions.add(firstDiscussion);
        discussions.add(secondDiscussion);
        var myIssue = new RadIssue("321", new RadAuthor(AUTHOR), "My Issue",
                RadIssue.State.OPEN, List.of(), List.of(), discussions);
        myIssue.project = getProject();
        myIssue.repo = firstRepo;
        return myIssue;
    }

    private RadDiscussion createDiscussion(String id, String authorId, String body) {
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of());
    }
}
