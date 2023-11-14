package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestPatches;
import static network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchListPanelTest.getTestProjects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class IssueListPanelTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String AUTHOR1 = "did:key:testAuthor1";
    private static final String AUTHOR2 = "did:key:testAuthor2";

    private RadicleToolWindow radicleToolWindow;
    private static List<RadIssue> issues;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        var api = replaceApiService();
        final var httpClient = api.getClient();
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = (HttpGet) i.getArgument(0);
            final StringEntity se;
            if (req.getURI().getPath().endsWith(PROJECTS_URL)) {
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects()));
            } else if (req.getURI().getPath().endsWith(ISSUES_URL)) {
                // request to fetch issues
                var query = req.getURI().getQuery();
                var parts = query.split("&");
                var state = parts[1].split("=")[1];
                var issue = getTestIssues().stream().filter(is -> is.state.status.equals(state)).toList();
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(issue));
            } else if (req.getURI().getPath().endsWith(PATCHES_URL)) {
                // request to fetch patches
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestPatches()));
            } else {
                // request to fetch specific project
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects().get(0)));
            }
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            final var resp = mock(CloseableHttpResponse.class);
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
        //Set issue content as selected in order to load the issues
        var contents = radicleToolWindow.contentManager.getContents();
        radicleToolWindow.contentManager.setSelectedContent(contents[1]);
        //Wait to load the issues
        Thread.sleep(100);
        executeUiTasks();
    }

    @Test
    public void testListPanel() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        var firstRadIssue = issueModel.get(0);
        assertThat(firstRadIssue.author.id).isEqualTo(issues.get(0).author.id);
        assertThat(firstRadIssue.title).isEqualTo(issues.get(0).title);
        assertThat(firstRadIssue.labels).isEqualTo(issues.get(0).labels);
    }

    @Test
    public void testfilterEmptyResults() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = "lala";
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        listPanel.updateListEmptyText(filterWithSearch);
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(0);
        var emptyText = listPanel.getList().getEmptyText();
        assertThat(emptyText.getText()).isEqualTo("Nothing found");
    }

    @Test
    public void testFilterByAuthor() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        filter.author = AUTHOR;
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        var issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(0).author.id);

        filter.author = AUTHOR1;
        listPanel.filterList(filter);

        issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(1).author.id);
    }

    @Test
    public void testFilterByProject() throws ExecutionException, InterruptedException {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get().get(0);
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);

        var issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(0).author.id);
    }

    @Test
    public void testFilterByAssignees() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        filter.assignee = (String) issues.get(0).assignees.get(1);
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);

        var issue = issueModel.get(0);
        assertThat(issue.id).isEqualTo(issues.get(0).id);

        issue = issueModel.get(1);
        assertThat(issue.id).isEqualTo(issues.get(1).id);
    }

    @Test
    public void testSearch() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        //Filter with title
        filterWithSearch.searchQuery = issues.get(0).title;
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        var issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(0).author.id);
    }

    @Test
    public void testState() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        filterWithSearch.state = RadIssue.State.OPEN.label;
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var issue = patchModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(0).author.id);

        filterWithSearch.state = RadIssue.State.CLOSED.label;
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        issue = patchModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(1).author.id);

        filterWithSearch.state = RadPatch.State.ARCHIVED.label;
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    @Test
    public void testTagDuplicates() throws ExecutionException, InterruptedException {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getLabels().get();
        assertThat(tags.size()).isEqualTo(4);
    }

    @Test
    public void testAssigneesDuplicates() throws ExecutionException, InterruptedException {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getAssignees().get();
        assertThat(tags.size()).isEqualTo(3);
    }

    @Test
    public void testTag() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        filterWithSearch.label = "tag1";
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var issue = patchModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(0).author.id);

        filterWithSearch.label = "firstTag";
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }


    public static List<RadIssue> getTestIssues() {
        var discussion = createDiscussion("123", AUTHOR, "Figure it out, i dont care");
        var discussion1 = createDiscussion("321", AUTHOR1, "This is a feature not a bug");
        var radIssue = new RadIssue("c5df12", new RadAuthor(AUTHOR), "Title1", RadIssue.State.OPEN, List.of(AUTHOR, AUTHOR1),
                List.of("tag1", "tag2"), List.of(discussion));
        var radIssue1 = new RadIssue("123ca", new RadAuthor(AUTHOR1), "Title", RadIssue.State.CLOSED, List.of(AUTHOR1, AUTHOR2),
                List.of("tag3", "tag4"), List.of(discussion1));
        issues = List.of(radIssue, radIssue1);
        return issues;
    }

    private static RadDiscussion createDiscussion(String id, String authorId, String body) {
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of(), List.of());
    }
}
