package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class IssueListPanelTest extends AbstractIT {
    public static final String URL = "/issues";
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String AUTHOR1 = "did:key:testAuthor1";
    private static final String AUTHOR2 = "did:key:testAuthor2";

    private RadicleToolWindow radicleToolWindow;
    private static List<RadIssue> issues;

    @Before
    public void setUpToolWindow() throws InterruptedException {
        issues = getTestIssues();
        radicleToolWindow = new RadicleToolWindow(new ProjectApi(httpClient));
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
        assertThat(firstRadIssue.author.id()).isEqualTo(issues.get(0).author.id());
        assertThat(secondRadIssue.author.id()).isEqualTo(issues.get(1).author.id());

        assertThat(firstRadIssue.title).isEqualTo(issues.get(0).title);
        assertThat(secondRadIssue.title).isEqualTo(issues.get(1).title);

        assertThat(firstRadIssue.tags).isEqualTo(issues.get(0).tags);
        assertThat(secondRadIssue.tags).isEqualTo(issues.get(1).tags);
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
        assertThat(issue.author.id()).isEqualTo(issues.get(0).author.id());

        filter.author = AUTHOR1;
        listPanel.filterList(filter);

        issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(1);
        issue = issueModel.get(0);
        assertThat(issue.author.id()).isEqualTo(issues.get(1).author.id());
    }

    @Test
    public void testFilterByProject() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get(0);
        listPanel.filterList(filter);

        var issueModel = listPanel.getModel();
        assertThat(issueModel.getSize()).isEqualTo(2);

        var issue = issueModel.get(0);
        assertThat(issue.author.id()).isEqualTo(issues.get(0).author.id());
    }

    @Test
    public void testFilterByAssignees() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var filter = new IssueListSearchValue();
        filter.assignee = issues.get(0).assignees.get(1);
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
        assertThat(issue.author.id()).isEqualTo(issues.get(0).author.id());
    }

    @Test
    public void testState() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        filterWithSearch.state = RadPatch.State.OPEN.status;
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var issue = patchModel.get(0);
        assertThat(issue.author.id()).isEqualTo(issues.get(0).author.id());

        filterWithSearch.state = RadPatch.State.CLOSED.status;
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        issue = patchModel.get(0);
        assertThat(issue.author.id()).isEqualTo(issues.get(1).author.id());

        filterWithSearch.state = RadPatch.State.MERGED.status;
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    @Test
    public void testTagDuplicates() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getTags();
        assertThat(tags.size()).isEqualTo(4);
    }

    @Test
    public void testAssigneesDuplicates() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var listPanel = controller.getIssueListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getAssignees();
        assertThat(tags.size()).isEqualTo(3);
    }

    @Test
    public void testTag() {
        var controller = (IssueTabController) radicleToolWindow.issueTabController;
        var filterWithSearch = new IssueListSearchValue();

        filterWithSearch.tag = "tag1";
        var listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var issue = patchModel.get(0);
        assertThat(issue.author.id()).isEqualTo(issues.get(0).author.id());

        filterWithSearch.tag = "firstTag";
        listPanel = controller.getIssueListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }


    public static List<RadIssue> getTestIssues() {
        var discussion = createDiscussion("123", AUTHOR, "Figure it out, i dont care");
        var discussion1 = createDiscussion("321", AUTHOR1, "This is a feature not a bug");
        var radIssue = new RadIssue("c5df12", new RadIssue.Author(AUTHOR), "Title1", RadIssue.State.OPEN, List.of(AUTHOR, AUTHOR1),
                List.of("tag1", "tag2"), List.of(discussion));
        var radIssue1 = new RadIssue("123ca", new RadIssue.Author(AUTHOR1), "Title", RadIssue.State.CLOSED, List.of(AUTHOR1, AUTHOR2),
                List.of("tag3", "tag4"), List.of(discussion1));
        issues = List.of(radIssue, radIssue1);
        return issues;
    }

    private static RadIssue.Discussion createDiscussion(String id, String authorId, String body) {
        return new RadIssue.Discussion(id, new RadIssue.Author(authorId), body, Instant.now(), "", List.of());
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
