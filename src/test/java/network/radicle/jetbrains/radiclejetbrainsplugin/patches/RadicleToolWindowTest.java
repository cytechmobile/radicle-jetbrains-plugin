package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.providers.ProjectApi;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class RadicleToolWindowTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private RadicleToolWindow radicleToolWindow;

    private List<RadPatch> getTestPatches() {
        var author = new RadPatch.Author(AUTHOR);
        var revision = new RadPatch.Revision("testRevision", "testDescription", "", "",
                List.of(), List.of(), Instant.now(), List.of(), List.of());
        var radPatch = new RadPatch("c5df12", "testPatch", author, "testDesc", "testTarget",
                List.of("tag1", "tag2"), RadPatch.State.OPEN, List.of(revision));
        return List.of(radPatch);
    }

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        StringEntity se = new StringEntity(new ObjectMapper().findAndRegisterModules().writeValueAsString(getTestPatches()));
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        when(httpResponse.getEntity()).thenReturn(se);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        radicleToolWindow = new RadicleToolWindow(new ProjectApi(httpClient));
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Wait to load the patch proposals
        Thread.sleep(2000);
    }

    @Test
    public void testFilterEmptyResults() {
        var controller = radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = "lala";
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);

        var patchModel = listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
        var emptyText = listPanel.getPatchesList().getEmptyText();
        assertThat(emptyText.getText()).isEqualTo("Nothing found");
    }

    @Test
    public void testFilterByAuthor() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var filter = new PatchListSearchValue();
        filter.author = AUTHOR;
        listPanel.filterList(filter);

        var patchModel = listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(AUTHOR);
    }

    @Test
    public void testFilterByProject() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var filter = new PatchListSearchValue();
        var searchVm = listPanel.getSearchVm();
        var projectNames = searchVm.getProjectNames();
        filter.project = projectNames.get(0);
        listPanel.filterList(filter);

        var patchModel = listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);

        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(AUTHOR);
    }

    @Test
    public void testSearch() {
        var controller = radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = AUTHOR;
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);

        var patchModel = listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author.id()).isEqualTo(AUTHOR);
    }

    @Test
    public void testFiltersData() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var searchVm = listPanel.getSearchVm();
        var filterAuthors = searchVm.getPeerIds();
        var projectNames = searchVm.getProjectNames();

        assertThat(projectNames.size()).isEqualTo(1);
        assertThat(projectNames.size()).isEqualTo(1);
        assertThat(projectNames.get(0)).contains("testRemote");
        assertThat(filterAuthors.get(0)).isEqualTo(AUTHOR);
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
