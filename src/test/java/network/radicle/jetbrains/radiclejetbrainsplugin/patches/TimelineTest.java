package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.UIUtil;
import git4idea.GitCommit;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchEditorProvider;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueListPanelTest.getTestIssues;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestPatches;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class TimelineTest extends AbstractIT {
    private static final Logger logger = LoggerFactory.getLogger(TimelineTest.class);
    private static final String AUTHOR = "did:key:testAuthor";
    private static String dummyComment = "Hello";
    private RadPatch patch;
    private PatchEditorProvider patchEditorProvider;
    private VirtualFile editorFile;
    private PatchTabController patchTabController;

    @Before
    public void beforeTest() throws IOException, InterruptedException {
        var api = replaceApiService();
        patch = createPatch();
        final var httpClient = api.getClient();
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = i.getArgument(0);
            StringEntity se;
            if ((req instanceof HttpPut) && ((HttpPut) req).getURI().getPath().contains("/sessions")) {
                se = new StringEntity("{}");
            } else if ((req instanceof HttpPatch) && ((HttpPatch) req).getURI().getPath().contains("/patches/" + patch.id)) {
                se = new StringEntity("{}");
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains("/patches/" + patch.id)) {
                patch.repo = null;
                patch.project = null;
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(patch));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().contains("/patches")) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestPatches()));
            } else if ((req instanceof HttpGet) && ((HttpGet) req).getURI().getPath().endsWith(IssueListPanelTest.URL)) {
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
            when(statusLine.getStatusCode()).thenReturn(200);
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
        patchTabController.createPatchProposalPanel(patch);
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
        Thread.sleep(200);
    }

    @Test
    public void testChangeTitle() throws InterruptedException {
        var timelineComponent = patchEditorProvider.getTimelineComponent();
        var titlePanel = timelineComponent.getHeaderPanel();
        var editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();

        var ef = UIUtil.findComponentOfType(titlePanel, EditorTextField.class);
        /* Test the header title */
        assertThat(ef.getText()).isEqualTo(patch.title);

        markAsShowing(ef.getParent());
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        final var editedTitle = "Edited title to " + UUID.randomUUID();
        ef.setText(editedTitle);
        final var prBtns = UIUtil.findComponentsOfType(titlePanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        final var prBtn = prBtns.get(1);
        /* click the button to edit the patch */
        patch.title = editedTitle;
        prBtn.doClick();
        /* Wait for the reload */
        timelineComponent.getLatch().await();
        var updatedPatchModel = patchTabController.getPatchModel();
        var updatedPatch = updatedPatchModel.getValue();
        assertThat(editedTitle).isEqualTo(updatedPatch.title);

        // Open createEditor
        patchEditorProvider.createEditor(getProject(), editorFile);
        timelineComponent = patchEditorProvider.getTimelineComponent();
        titlePanel = timelineComponent.getHeaderPanel();
        editBtn = UIUtil.findComponentOfType(titlePanel, InlineIconButton.class);
        //send event that we clicked edit
        editBtn.getActionListener().actionPerformed(new ActionEvent(editBtn, 0, ""));
        executeUiTasks();
        ef = UIUtil.findComponentOfType(titlePanel, EditorTextField.class);
        assertThat(ef.getText()).isEqualTo(editedTitle);
    }

    @Test
    public void testDescSection() {
        var descSection = patchEditorProvider.getTimelineComponent().getComponentsFactory().getDescSection();
        var elements = UIUtil.findComponentsOfType(descSection, BaseHtmlEditorPane.class);
        var timeline = "";
        for (var el : elements) {
            timeline += el.getText();
        }
        assertThat(timeline).contains(patch.description);
        assertThat(timeline).contains(patch.author.id);
    }

    @Test
    public void testRevSection() {
        executeUiTasks();
        var revisionSection = patchEditorProvider.getTimelineComponent().getRevisionSection();
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
        var elements = UIUtil.findComponentsOfType(revisionSection, BaseHtmlEditorPane.class);
        var comments = "";
        for (var el : elements) {
            comments += el.getText();
        }
        assertThat(comments).contains(patch.revisions.get(0).discussions().get(0).body);
        assertThat(comments).contains(patch.revisions.get(0).id());
        assertThat(comments).contains(patch.revisions.get(1).discussions().get(0).body);
        assertThat(comments).contains(patch.revisions.get(1).id());
    }

    @Test
    public void testComment() throws InterruptedException {
        // Clear previous commands
        radStub.commands.clear();
        executeUiTasks();
        var timelineComponent = patchEditorProvider.getTimelineComponent();
        var commentPanel = timelineComponent.getCommentPanel();
        var ef = UIUtil.findComponentOfType(commentPanel, EditorTextField.class);
        markAsShowing(ef.getParent());
        for (var hl : ef.getParent().getHierarchyListeners()) {
            hl.hierarchyChanged(new HierarchyEvent(ef, 0, ef, ef.getParent(), HierarchyEvent.SHOWING_CHANGED));
        }
        executeUiTasks();
        assertThat(ef.getText()).isEmpty();
        ef.setText(dummyComment);
        final var prBtns = UIUtil.findComponentsOfType(commentPanel, JButton.class);
        assertThat(prBtns).hasSizeGreaterThanOrEqualTo(1);
        final var prBtn = prBtns.get(1);
        prBtn.doClick();
        //assertThat(cmd.getCommandLineString()).contains("comment " + patch.id + " --message " + dummyComment);
        // Check that the comment is visible in the editor
        executeUiTasks();
        var revisionSection = patchEditorProvider.getTimelineComponent().getRevisionSection();
        String comments = "";
        for (int i = 0; i < 10; i++) {
            var elements = UIUtil.findComponentsOfType(revisionSection, BaseHtmlEditorPane.class);
            comments = "";
            for (var el : elements) {
                comments += el.getText();
            }
            if (comments.contains(dummyComment)) {
                break;
            }
            Thread.sleep(200);
        }
        assertThat(comments).contains(patch.revisions.get(0).discussions().get(0).body);
        assertThat(comments).contains(patch.revisions.get(1).discussions().get(0).body);
        assertThat(comments).contains(patch.revisions.get(1).id());
    }

    private RadPatch createPatch() {
        var firstCommit = commitHistory.get(0);
        var secondCommit = commitHistory.get(1);
        var firstDiscussion = createDiscussion("123", "123", "hello");
        var secondDiscussion = createDiscussion("321", "321", "hello back");
        var firstRev = createRevision("testRevision1", "testRevision1", firstCommit, firstDiscussion);
        var secondRev = createRevision("testRevision2", "testRevision1", secondCommit, secondDiscussion);
        var myPatch = new RadPatch("c5df12", "testPatch", new RadAuthor(AUTHOR), "testDesc",
                "testTarget", List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(firstRev, secondRev));
        myPatch.project = getProject();
        myPatch.repo = firstRepo;
        return myPatch;
    }

    private RadPatch.Revision createRevision(String id, String description, GitCommit commit,
                                             RadDiscussion discussion) {
        var fistCommitChanges = (ArrayList) commit.getChanges();
        var firstChange = (Change) fistCommitChanges.get(0);
        var base = firstChange.getBeforeRevision().getRevisionNumber().asString();
        return new RadPatch.Revision(id, description, base, commit.getId().asString(),
                List.of("branch"), List.of(), Instant.now(), List.of(discussion), List.of());
    }

    private RadDiscussion createDiscussion(String id, String authorId, String body) {
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of());
    }

}
