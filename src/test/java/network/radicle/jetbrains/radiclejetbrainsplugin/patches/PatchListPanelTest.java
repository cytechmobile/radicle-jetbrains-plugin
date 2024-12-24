package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class PatchListPanelTest extends AbstractIT {
    RadicleToolWindow radicleToolWindow;
    List<RadPatch> patches;

    @Before
    public void setUpToolWindow() throws Exception {
        patches = getTestPatches();

        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Wait to load the patch proposals
        Thread.sleep(100);
        executeUiTasks();
    }

    @Test
    public void testFilterEmptyResults() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        //Filter with search (peer id)
        var filterWithSearch = new PatchListSearchValue();
        filterWithSearch.searchQuery = "lala";
        listPanel.filterList(filterWithSearch);
        executeUiTasks();
        listPanel.updateListEmptyText(filterWithSearch);

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
        var emptyText = listPanel.getList().getEmptyText();
        assertThat(emptyText.getText()).isEqualTo("Nothing found");
    }

    @Test
    public void testFilterByAuthor() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filter = new PatchListSearchValue();
        filter.author = patches.getFirst().author.id;
        listPanel.filterList(filter);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.getFirst().author.id);

        filter.author = patches.get(1).author.id;
        listPanel.filterList(filter);
        executeUiTasks();

        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(1).author.id);
    }

    @Test
    public void testFilterByProject() throws ExecutionException, InterruptedException {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filter = new PatchListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get().getFirst();
        listPanel.filterList(filter);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);

        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isIn(patches.get(0).author.id, patches.get(1).author.id);
    }

    @Test
    public void testSearch() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        //Filter with search (peer id)
        var filterWithSearch = new PatchListSearchValue();
        filterWithSearch.searchQuery = patches.getFirst().author.id;
        listPanel.filterList(filterWithSearch);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.getFirst().author.id);

        filterWithSearch.searchQuery = patches.getFirst().title;
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isIn(patches.get(0).author.id, patches.get(1).author.id);
    }

    @Test
    public void testFiltersData() throws ExecutionException, InterruptedException {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var searchVm = listPanel.getSearchVm();
        var filterAuthors = searchVm.getAuthors();
        var projectNames = searchVm.getProjectNames();

        assertThat(projectNames.get().size()).isEqualTo(1);
        assertThat(projectNames.get().size()).isEqualTo(1);
        assertThat(projectNames.get().getFirst()).contains("testRemote");

        assertThat(filterAuthors.get().get(0)).isIn(patches.get(0).author.id, patches.get(1).author.id);
        assertThat(filterAuthors.get().get(1)).isIn(patches.get(0).author.id, patches.get(1).author.id);
    }

    @Test
    public void testListPanel() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var firstPatch = patchModel.get(0);
        assertThat(firstPatch.author.id).isEqualTo(patches.getFirst().author.id);
        assertThat(firstPatch.title).isEqualTo(patches.getFirst().title);
        assertThat(firstPatch.labels).isEqualTo(patches.getFirst().labels);
    }

    @Test
    public void testState() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filterWithSearch = new PatchListSearchValue();
        filterWithSearch.state = RadPatch.State.OPEN.label;
        listPanel.filterList(filterWithSearch);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.getFirst().author.id);

        filterWithSearch.state = RadPatch.State.DRAFT.label;
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(1).author.id);

        filterWithSearch.state = RadPatch.State.ARCHIVED.label;
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    @Test
    public void testTagDuplicates() throws ExecutionException, InterruptedException {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getLabels();
        assertThat(tags.get().size()).isEqualTo(4);
    }

    @Test
    public void testTag() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filterWithSearch = new PatchListSearchValue();

        filterWithSearch.label = "tag1";
        listPanel.filterList(filterWithSearch);
        executeUiTasks();
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isIn(patches.get(0).author.id, patches.get(1).author.id);

        filterWithSearch.label = "firstTag";
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
         radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(1).author.id);

        filterWithSearch.label = "unknownTag";
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    public static List<RadProject> getTestProjects() {
        final var author1 = new RadAuthor("did:key:test1", "test1");
        final var author2 = new RadAuthor("did:key:test2", "test2");
        final var author3 = new RadAuthor("did:key:test3", "test3");
        return List.of(new RadProject("test-rad-project", "test project", "test project description", "main", List.of(author1, author2, author3)),
                new RadProject("test-rad-project-second", "test project 2", "test project 2 description", "main", List.of(author1)));
    }

    public static List<RadPatch> getTestPatches() {
        final var author1 = new RadAuthor("did:key:test1", "test1");
        final var author2 = new RadAuthor("did:key:test2", "test2");

        var reviewMap = new HashMap<String, RadPatch.Review>();
        reviewMap.put("1", new RadPatch.Review("1", author1, RadPatch.Review.Verdict.ACCEPT, "test",
                new RadPatch.DiscussionObj(new HashMap<>(), List.of()), Instant.now()));

        var revision = new RadPatch.Revision("testRevision", author1, List.of(), List.of(), "", "",
                List.of(), Instant.now(), new RadPatch.DiscussionObj(new HashMap<>(), List.of()), reviewMap);

        var revMap = new HashMap<String, RadPatch.Revision>();
        revMap.put(revision.id(), revision);
        var radPatch = new RadPatch("c5df12", new RadProject("test-rad-project", "test-rad-project", "", "main", List.of(author1)),
                author1, "testPatch", author1, "testTarget", List.of("tag1", "tag2"), RadPatch.State.OPEN, revMap);

        var radPatch2 = new RadPatch("c4d12", new RadProject("test-rad-project-second", "test-rad-project-second", "", "main", List.of(author1)),
                author1, "secondProposal", author2, "testTarget", List.of("firstTag", "secondTag", "tag1"), RadPatch.State.DRAFT, revMap);
        return List.of(radPatch, radPatch2);
    }
}
