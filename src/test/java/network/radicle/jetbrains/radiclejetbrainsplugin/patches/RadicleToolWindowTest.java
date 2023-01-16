package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.openapi.project.Project;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.SeedNode;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.RadStub.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RadicleToolWindowTest extends AbstractIT {

    private RadicleToolWindow radicleToolWindow;
    @Before
    public void setUpToolWindow() throws InterruptedException {
        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        //Wait to load the patch proposals
        Thread.sleep(2000);
    }

    @Test
    public void testSeedNodeSelection() throws InterruptedException {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var seedNodeComboBox = listPanel.getSeedNodeComboBox();

        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        var selectedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track --seed http://" + selectedNode.host + " --remote");

        for (int i = 1; i < seedNodeComboBox.getItemCount(); i++) {
            seedNodeComboBox.setSelectedIndex(i);
            cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
            selectedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
            assertCmd(cmd);
            assertThat(cmd.getCommandLineString()).contains("track --seed http://" + selectedNode.host + " --remote");
        }
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
        filter.peerId = FIRST_PEER_ID;
        listPanel.filterList(filter);

        var patchModel =  listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.peerId).isEqualTo(FIRST_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(FIRST_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(radStub.firstCommitHash);
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
        assertThat(patchModel.getSize()).isEqualTo(2);

        var radPatch = patchModel.get(0);
        assertThat(radPatch.peerId).isEqualTo(FIRST_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(FIRST_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(radStub.firstCommitHash);

        radPatch = patchModel.get(1);
        assertThat(radPatch.peerId).isEqualTo(SECOND_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(SECOND_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(SECOND_COMMIT_HASH);
    }

    @Test
    public void testSearch() {
        var controller = radicleToolWindow.patchTabController;
        var filterWithSearch = new PatchListSearchValue();

        //Filter with search (peer id)
        filterWithSearch.searchQuery = FIRST_PEER_ID;
        var listPanel = controller.getPatchListPanel();
        listPanel.filterList(filterWithSearch);

        var patchModel =  listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(1);
        var radPatch = patchModel.get(0);
        assertThat(radPatch.peerId).isEqualTo(FIRST_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(FIRST_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(radStub.firstCommitHash);

        //Filter with search (branch)
        filterWithSearch = new PatchListSearchValue();
        filterWithSearch.searchQuery = FIRST_BRANCH_NAME;
        listPanel.filterList(filterWithSearch);

        patchModel =  listPanel.getPatchModel();
        assertThat(patchModel.getSize()).isEqualTo(2);
        radPatch = patchModel.get(0);
        assertThat(radPatch.peerId).isEqualTo(FIRST_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(FIRST_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(radStub.firstCommitHash);

        radPatch = patchModel.get(1);
        assertThat(radPatch.peerId).isEqualTo(SECOND_PEER_ID);
        assertThat(radPatch.branchName).isEqualTo(SECOND_BRANCH_NAME);
        assertThat(radPatch.commitHash).isEqualTo(SECOND_COMMIT_HASH);
    }

    @Test
    public void testFiltersData() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var searchVm = listPanel.getSearchVm();
        var filterPeerIds = searchVm.getPeerIds();
        var projectNames = searchVm.getProjectNames();

        assertThat(projectNames.size()).isEqualTo(1);
        assertThat(projectNames.get(0)).contains("testRemote");
        assertThat(filterPeerIds.get(0)).isEqualTo(FIRST_PEER_ID);
        assertThat(filterPeerIds.get(1)).isEqualTo(SECOND_PEER_ID);
    }

    @Test
    public void testProposalPanel() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var radPatch = listPanel.getRadPatches().get(0);
        controller.createPatchProposalPanel(radPatch);
        var patchProposalPanel = controller.getPatchProposalPanel();
        // Wait to load the changes
        while(patchProposalPanel.patchChanges.getValue().isEmpty() || patchProposalPanel.patchCommits.getValue().isEmpty()) {

        }
        assertThat(patchProposalPanel.patchChanges.getValue().size()).isEqualTo(1);
        assertThat(patchProposalPanel.patchCommits.getValue().size()).isEqualTo(1);
    }

    @Test
    public void testRefreshButton() throws InterruptedException {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var seedNodeComboBox = listPanel.getSeedNodeComboBox();

        radStub.commands.poll(10, TimeUnit.SECONDS);

        var refreshAction = listPanel.new RefreshSeedNodeAction();
        refreshAction.refreshSeedNodesAndProposals();
        var cmd = radStub.commands.poll(10, TimeUnit.SECONDS);
        var selectedNode = (SeedNode) seedNodeComboBox.getSelectedItem();
        assertCmd(cmd);
        assertThat(cmd.getCommandLineString()).contains("track --seed http://" + selectedNode.host + " --remote");
    }

    @Test
    public void testPatchProposalsParsing() {
        var controller = radicleToolWindow.patchTabController;
        var listPanel = controller.getPatchListPanel();
        var loadedRadPatches = listPanel.getRadPatches();
        assertThat(loadedRadPatches).hasSize(2);

        assertThat(loadedRadPatches.get(0).peerId).isEqualTo(FIRST_PEER_ID);
        assertThat(loadedRadPatches.get(0).branchName).isEqualTo(FIRST_BRANCH_NAME);
        assertThat(loadedRadPatches.get(0).commitHash).isEqualTo(radStub.firstCommitHash);

        assertThat(loadedRadPatches.get(1).peerId).isEqualTo(radStub.SECOND_PEER_ID);
        assertThat(loadedRadPatches.get(1).branchName).isEqualTo(radStub.SECOND_BRANCH_NAME);
        assertThat(loadedRadPatches.get(1).commitHash).isEqualTo(radStub.SECOND_COMMIT_HASH);

        assertThat(loadedRadPatches.get(0).repo.getRoot().getPath()).isEqualTo(firstRepo.getRoot().getPath());
        assertThat(loadedRadPatches.get(1).repo.getRoot().getPath()).isEqualTo(firstRepo.getRoot().getPath());
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
