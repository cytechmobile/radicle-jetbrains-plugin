package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
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
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class PatchListPanelTest extends AbstractIT {
    public static final String AUTHOR = "did:key:testAuthor";
    public static final String AUTHOR1 = "did:key:testAuthor1";
    private static List<RadPatch> patches;
    private RadicleToolWindow radicleToolWindow;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        var api = replaceApiService();
        final var httpClient = api.getClient();
        patches = getTestPatches();
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = (HttpGet) i.getArgument(0);
            final StringEntity se;
            if (req.getURI().getPath().endsWith(PROJECTS_URL)) {
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects()));
            } else if (req.getURI().getPath().endsWith(PATCHES_URL)) {
                // request to fetch patches
                var query = req.getURI().getQuery();
                var parts = query.split("&");
                var state = parts[1].split("=")[1];
                var patch = getTestPatches().stream().filter(p -> p.state.status.equals(state)).toList();
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(patch));
            } else if (req.getURI().getPath().endsWith(ISSUES_URL)) {
                se = new StringEntity("[]");
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
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
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
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filter = new PatchListSearchValue();
        filter.author = AUTHOR;
        listPanel.filterList(filter);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);

        filter.author = AUTHOR1;
        listPanel.filterList(filter);
        executeUiTasks();

        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(1).author.id);
    }

    @Test
    public void testFilterByProject() throws ExecutionException, InterruptedException {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filter = new PatchListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get().get(0);
        listPanel.filterList(filter);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);

        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);
    }

    @Test
    public void testSearch() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        //Filter with search (peer id)
        var filterWithSearch = new PatchListSearchValue();
        filterWithSearch.searchQuery = AUTHOR;
        listPanel.filterList(filterWithSearch);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);

        filterWithSearch.searchQuery = patches.get(0).title;
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);
    }

    @Test
    public void testFiltersData() throws ExecutionException, InterruptedException {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var searchVm = listPanel.getSearchVm();
        var filterAuthors = searchVm.getAuthors();
        var projectNames = searchVm.getProjectNames();

        assertThat(projectNames.get().size()).isEqualTo(1);
        assertThat(projectNames.get().size()).isEqualTo(1);
        assertThat(projectNames.get().get(0)).contains("testRemote");
        assertThat(filterAuthors.get().get(0)).isEqualTo(patches.get(0).author.id);
    }

    @Test
    public void testListPanel() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var firstPatch = patchModel.get(0);
        assertThat(firstPatch.author.id).isEqualTo(patches.get(0).author.id);
        assertThat(firstPatch.title).isEqualTo(patches.get(0).title);
        assertThat(firstPatch.labels).isEqualTo(patches.get(0).labels);
    }

    @Test
    public void testState() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filterWithSearch = new PatchListSearchValue();
        filterWithSearch.state = RadPatch.State.OPEN.label;
        listPanel.filterList(filterWithSearch);
        executeUiTasks();

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);

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
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getLabels();
        assertThat(tags.get().size()).isEqualTo(4);
    }

    @Test
    public void testTag() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        executeUiTasks();

        var filterWithSearch = new PatchListSearchValue();

        filterWithSearch.label = "tag1";
        listPanel.filterList(filterWithSearch);
        executeUiTasks();
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id).isEqualTo(patches.get(0).author.id);

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
        return List.of(new RadProject("test-rad-project", "test project", "test project description",
                        "master", List.of("did:key:test", "did:key:assignee2", "did:key:assignee3")),
                new RadProject("test-rad-project-second", "test project 2", "test project 2 description",
                        "master", List.of("did:key:test")));
    }

    public static List<RadPatch> getTestPatches() {
        var revision = new RadPatch.Revision("testRevision", "testDescription", "", "",
                List.of(), List.of(), Instant.now(), List.of(), List.of(), new RadAuthor(UUID.randomUUID().toString()));

        var radPatch = new RadPatch("c5df12", "test-rad-project", "testPatch", new RadAuthor(AUTHOR), "testTarget",
                List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(revision));

        var radPatch2 = new RadPatch("c4d12", "test-rad-project-second", "secondProposal", new RadAuthor(AUTHOR1),
                "testTarget", List.of("firstTag", "secondTag", "tag1"), RadPatch.State.DRAFT, List.of(revision));
        patches = List.of(radPatch, radPatch2);
        return patches;
    }
}
