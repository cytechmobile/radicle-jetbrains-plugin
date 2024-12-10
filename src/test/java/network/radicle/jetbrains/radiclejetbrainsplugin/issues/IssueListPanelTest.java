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
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class IssueListPanelTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String AUTHOR1 = "did:key:testAuthor1";
    private static final String AUTHOR2 = "did:key:testAuthor2";

    private RadicleToolWindow radicleToolWindow;
    private static List<RadIssue> issues;

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
    }

    @Test
    public void testFilterByAssignees() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        filter.assignee = issues.get(0).assignees.get(1).id;
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);
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
        var discussion = createDiscussion(UUID.randomUUID().toString(), AUTHOR, "Figure it out, i dont care");
        var discussion1 = createDiscussion(UUID.randomUUID().toString(), AUTHOR1, "This is a feature not a bug");
        var radIssue = new RadIssue("c5df12", new RadAuthor(AUTHOR), "Title1", RadIssue.State.OPEN, List.of(new RadAuthor(AUTHOR), new RadAuthor(AUTHOR1)),
                List.of("tag1", "tag2"), List.of(discussion));
        var radIssue1 = new RadIssue("123ca", new RadAuthor(AUTHOR1), "Title", RadIssue.State.CLOSED, List.of(new RadAuthor(AUTHOR1), new RadAuthor(AUTHOR2)),
                List.of("tag3", "tag4"), List.of(discussion1));
        issues = List.of(radIssue, radIssue1);
        return issues;
    }

    private static RadDiscussion createDiscussion(String id, String authorId, String body) {
        var edit = new RadPatch.Edit(new RadAuthor("myTestAuthor"), "", Instant.now(), List.of());
        var edits = List.of(edit);
        return new RadDiscussion(id, new RadAuthor(authorId), body, Instant.now(), "", List.of(), List.of(), null, edits);
    }
}
