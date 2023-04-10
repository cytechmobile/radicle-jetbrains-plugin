package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class RadicleToolWindowTest extends AbstractIT {
    private static final String AUTHOR = "did:key:testAuthor";
    private RadicleToolWindow radicleToolWindow;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        StringEntity se = new StringEntity("[{\"id\":\"c5df12\",\"author\":{\"id\":\"" + AUTHOR + "\"},\"title\":\"My new project\"," +
                "\"description\":\"Hello this is a description\",\"state\":{\"status\":\"open\"}," +
                "\"target\":\"delegates\",\"tags\":[],\"revisions\":[{\"id\":\"testRevision\",\"description\":\"\"," +
                "\"base\":\"9de952f92e27b67b43d89230e2d68caa0794b5a9\",\"oid\":\"b2a64eb1f42fd757a5ad03dfc5e13727ad52cb6e\"," +
                "\"refs\":[],\"merges\":[],\"discussions\":[],\"timestamp\":1680864771,\"reviews\":[]}," +
                "{\"id\":\"14e35ef75f6c7b73f965004f6f985934ba13bcd9\",\"description\":\"Add README, just for the fun\",\"base\":" +
                "\"9de952f92e27b67b43d89230e2d68caa0794b5a9\",\"oid\":\"84fb6549088427b958903eafa52b6559b20756de\"," +
                "\"refs\":[\"refs/heads/new_branch\"],\"merges\":[],\"discussions\":[{\"id\":\"c37274314cbe28efdf7c33abcaa01b3473797985\",\"author\":" +
                "{\"id\":\"did:key:z6MkwM5cMjgsvTcUyEtUAfo1bWzGS9o41zXRcy7eqaZubwzS\"}," +
                "\"body\":\"TestMsg!\",\"reactions\":[],\"timestamp\":1680867331,\"replyTo\":null},{\"id\":\"538fe004d02e84e554c3b49b70567bdd59cd48c5\"," +
                "\"author\":{\"id\":\"did:key:z6MkwM5cMjgsvTcUyEtUAfo1bWzGS9o41zXRcy7eqaZubwzS\"}," +
                "\"body\":\"This is my Reply\",\"reactions\":[],\"timestamp\":1680867502,\"replyTo\":\"c37274314cbe28efdf7c33abcaa01b3473797985\"}]," +
                "\"timestamp\":1680865493,\"reviews\":[]}]}]");
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

        var patchModel =  listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(0);
        var emptyText = listPanel.getPatchesList().getEmptyText();
        assertThat(emptyText.getText()).isEqualTo("Nothing found");
    }
    @Test
    public void testFilterByPeerId() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var filter = new PatchListSearchValue();
        filter.author = AUTHOR;
        listPanel.filterList(filter);

       var patchModel =  listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.author).isEqualTo(AUTHOR);
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

       var patchModel =  listPanel.getPatchModel();
       assertThat(patchModel.getSize()).isEqualTo(1);

       var radPatch = patchModel.get(0);
       assertThat(radPatch.author).isEqualTo(AUTHOR);
    }

    @Test
    public void testSearch() {
        var controller = radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = AUTHOR;
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);

       var patchModel =  listPanel.getPatchModel();
       assertThat(patchModel.getSize()).isEqualTo(1);
       var radPatch = patchModel.get(0);
       assertThat(radPatch.author).isEqualTo(AUTHOR);
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
