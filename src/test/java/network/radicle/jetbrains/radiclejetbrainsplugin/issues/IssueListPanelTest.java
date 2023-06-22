package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestPatches;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class IssueListPanelTest extends AbstractIT {
    public static final String URL = "/issues";
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String AUTHOR1 = "did:key:testAuthor1";

    private RadicleToolWindow radicleToolWindow;
    private static List<RadIssue> issues;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        var api = replaceApiService();
        final var httpClient = api.getClient();
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = (HttpGet) i.getArgument(0);
            final StringEntity se;
            if (!Strings.isNullOrEmpty(req.getURI().getQuery())) {
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects()));
            } else if (req.getURI().getPath().endsWith(IssueListPanelTest.URL)) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestIssues()));
            } else if (req.getURI().getPath().endsWith(PatchListPanelTest.URL)) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestPatches()));
            } else {
                // request to fetch specific project
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects().get(0)));
            }
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            final var resp = mock(HttpResponse.class);
            when(resp.getEntity()).thenReturn(se);
            final var statusLine = mock(StatusLine.class);
            when(resp.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(200);
            return resp;
        });
        issues = getTestIssues();
        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Wait to load the issues
        Thread.sleep(2000);
    }

    @Test
    public void testListPanel() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);
        var firstRadIssue = issueModel.get(0);
        var secondRadIssue = issueModel.get(1);
        assertThat(firstRadIssue.author.id).isEqualTo(issues.get(0).author.id);
        assertThat(secondRadIssue.author.id).isEqualTo(issues.get(1).author.id);

        assertThat(firstRadIssue.title).isEqualTo(issues.get(0).title);
        assertThat(secondRadIssue.title).isEqualTo(issues.get(1).title);

        assertThat(firstRadIssue.tags).isEqualTo(issues.get(0).tags);
        assertThat(secondRadIssue.tags).isEqualTo(issues.get(1).tags);
    }

    public static List<RadIssue> getTestIssues() {
        var discussion = createDiscussion("123", AUTHOR, "Figure it out, i dont care");
        var discussion1 = createDiscussion("321", AUTHOR1, "This is a feature not a bug");
        var radIssue = new RadIssue("c5df12", new RadAuthor(AUTHOR), "Title", RadIssue.State.OPEN, List.of(),
                List.of("tag1", "tag2"), List.of(discussion));
        var radIssue1 = new RadIssue("123ca", new RadAuthor(AUTHOR), "Title", RadIssue.State.CLOSED, List.of(),
                List.of("tag3", "tag4"), List.of(discussion1));
        issues = List.of(radIssue, radIssue1);
        return issues;
    }

    private static RadDiscussion createDiscussion(String id, String authorId, String body) {
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of());
    }

    public static class MockToolWindow extends ToolWindowHeadlessManagerImpl.MockToolWindow {
        public MockToolWindow(@NotNull Project project) {
            super(project);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }
}
