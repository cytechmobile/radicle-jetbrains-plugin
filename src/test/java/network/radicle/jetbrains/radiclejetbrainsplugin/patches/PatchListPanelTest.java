package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class PatchListPanelTest extends AbstractIT {
    public static final String URL = "/patches";
    private static final String AUTHOR = "did:key:testAuthor";
    private static final String AUTHOR1 = "did:key:testAuthor1";
    private RadicleToolWindow radicleToolWindow;
    private static List<RadPatch> patches;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        var api = replaceApiService();
        final var httpClient = api.getClient();
        patches = getTestPatches();
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = (HttpGet) i.getArgument(0);
            final StringEntity se;
            if (!Strings.isNullOrEmpty(req.getURI().getQuery())) {
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(getTestProjects()));
            } else if (req.getURI().getPath().endsWith("/patches")) {
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

        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Wait to load the patch proposals
        Thread.sleep(100);
    }

    @Test
    public void testFilterEmptyResults() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = "lala";
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
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
        var filter = new PatchListSearchValue();
        filter.author = AUTHOR;
        listPanel.filterList(filter);

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());

        filter.author = AUTHOR1;
        listPanel.filterList(filter);

        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(1).author.id());
    }

    @Test
    public void testFilterByProject() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var filter = new PatchListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get(0);
        listPanel.filterList(filter);

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);

        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());
    }

    @Test
    public void testSearch() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = AUTHOR;
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);

        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());

        filterWithSearch.searchQuery = patches.get(0).title;
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());
    }

    @Test
    public void testFiltersData() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var searchVm = listPanel.getSearchVm();
        var filterAuthors = searchVm.getAuthors();
        var projectNames = searchVm.getProjectNames();

        assertThat(projectNames.size()).isEqualTo(1);
        assertThat(projectNames.size()).isEqualTo(1);
        assertThat(projectNames.get(0)).contains("testRemote");
        assertThat(filterAuthors.get(0)).isEqualTo(patches.get(0).author.id());
    }

    @Test
    public void testState() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        filterWithSearch.state = RadPatch.State.OPEN.status;
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());

        filterWithSearch.state = RadPatch.State.CLOSED.status;
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(1).author.id());

        filterWithSearch.state = RadPatch.State.MERGED.status;
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    @Test
    public void testTagDuplicates() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var searchVm = listPanel.getSearchVm();
        var tags = searchVm.getTags();
        assertThat(tags.size()).isEqualTo(4);
    }

    @Test
    public void testTag() {
        var controller = (PatchTabController) radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        filterWithSearch.tag = "tag1";
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        var patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(0).author.id());

        filterWithSearch.tag = "firstTag";
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(patches.get(1).author.id());

        filterWithSearch.tag = "unknownTag";
        listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);
        patchModel = listPanel.getModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
    }

    public static List<RadProject> getTestProjects() {
        return List.of(new RadProject("test-rad-project", "test project", "test project description", "main"),
                new RadProject("test-rad-project-second", "test project 2", "test project 2 description", "main"));
    }

    public static List<RadPatch> getTestPatches() {
        var revision = new RadPatch.Revision("testRevision", "testDescription", "", "",
                List.of(), List.of(), Instant.now(), List.of(), List.of());

        var radPatch = new RadPatch("c5df12", "testPatch", new RadPatch.Author(AUTHOR), "testDesc", "testTarget",
                List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(revision));

        var radPatch2 = new RadPatch("c4d12", "secondProposal", new RadPatch.Author(AUTHOR1),
                "My description", "testTarget", List.of("firstTag", "secondTag", "tag1"),
                RadPatch.State.CLOSED, List.of(revision));
        patches = List.of(radPatch, radPatch2);
        return patches;
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
