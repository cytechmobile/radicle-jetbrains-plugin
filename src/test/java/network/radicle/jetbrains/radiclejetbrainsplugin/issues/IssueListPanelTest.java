package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadDiscussion;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class IssueListPanelTest extends AbstractIT {

    RadicleToolWindow radicleToolWindow;
    List<RadIssue> issues;

    @Before
    public void setUpToolWindow() throws InterruptedException {
        issues = getTestIssues();
        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Set issue content as selected in order to load the issues
        var contents = radicleToolWindow.getContentManager().getContents();
        radicleToolWindow.getContentManager().setSelectedContent(contents[1]);
        //Wait to load the issues
        Thread.sleep(100);
        executeUiTasks();
    }

    @Test
    public void testListPanel() {
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        var firstRadIssue = issueModel.get(0);
        assertThat(firstRadIssue.author.id).isEqualTo(issues.getFirst().author.id);
        assertThat(firstRadIssue.title).isEqualTo(issues.getFirst().title);
        assertThat(firstRadIssue.labels).isEqualTo(issues.getFirst().labels);
    }

    @Test
    public void testFilterEmptyResults() {
        var controller = radicleToolWindow.issueTabController;
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
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();

        filter.author = issues.getFirst().author.id;
        listPanel.filterList(filter);
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        var issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.getFirst().author.id);

        filter.author = issues.get(1).author.id;
        listPanel.filterList(filter);

        issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        issue = issueModel.get(0);
        assertThat(issue.author.id).isEqualTo(issues.get(1).author.id);
    }

    @Test
    public void testFilterByProject() throws ExecutionException, InterruptedException {
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get().getFirst();
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);
    }

    @Test
    public void testFilterByAssignees() throws Exception {
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        filter.assignee = issues.getFirst().assignees.get(1).id;
        listPanel.filterList(filter);
        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);
    }

    @Test
    public void testSearch() {
        var controller = radicleToolWindow.issueTabController;
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
        var controller = radicleToolWindow.issueTabController;
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
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getLabels().get();
        assertThat(tags.size()).isEqualTo(4);
    }

    @Test
    public void testAssigneesDuplicates() throws ExecutionException, InterruptedException {
        var controller = radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getAssignees().get();
        assertThat(tags.size()).isEqualTo(3);
    }

    @Test
    public void testTag() {
        var controller =  radicleToolWindow.issueTabController;
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
        final var author1 = new RadAuthor("did:key:testAuthor1", "testAuthor1");
        final var author2 = new RadAuthor("did:key:testAuthor2", "testAuthor2");
        final var author3 = new RadAuthor("did:key:testAuthor3", "testAuthor3");
        var discussion = createDiscussion(author1, "Figure it out, i dont care");
        var discussion1 = createDiscussion(author2, "This is a feature not a bug");
        var radIssue = new RadIssue("c5df12", author1, "Title1", RadIssue.State.OPEN, List.of(author1, author2),
                List.of("tag1", "tag2"), List.of(discussion));
        var radIssue1 = new RadIssue("123ca", author2, "Title", RadIssue.State.CLOSED, List.of(author2, author3),
                List.of("tag3", "tag4"), List.of(discussion1));
        return List.of(radIssue, radIssue1);
    }

    private static RadDiscussion createDiscussion(RadAuthor author, String body) {
        var edit = new RadPatch.Edit(randomAuthor(), "", Instant.now(), List.of());
        var edits = List.of(edit);
        return new RadDiscussion(randomId(), author, body, Instant.now(), "", List.of(), List.of(), null, edits);
    }
}
