package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.BaseHtmlEditorPane;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.UIUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchEditorProvider;
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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitTestUtil.addRadRemote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class PatchProposalPanelTest extends AbstractIT {
    private static final Logger logger = Logger.getInstance(MergePatchAction.class);

    private RadicleToolWindow radicleToolWindow;
    private static List<RadPatch> patches;
    private static List<RadProject> projects;

    RadPatch patch;
    PatchTabController patchTabController;
    VirtualFile editorFile;
    PatchEditorProvider patchEditorProvider;

    @Before
    public void setUpToolWindow() throws InterruptedException, IOException {
        var api = replaceApiService();
        final var httpClient = api.getClient();
        projects = PatchListPanelTest.getTestProjects();
        patches = PatchListPanelTest.getTestPatches();
        patch = new RadPatch(patches.get(0));
        patch.repo = firstRepo;
        patch.project = getProject();
        patch.radProject.defaultBranch = projects.get(0).defaultBranch;
        when(httpClient.execute(any())).thenAnswer((i) -> {
            var req = (HttpGet) i.getArgument(0);
            final StringEntity se;
            logger.warn("mocked request:" + req.getURI());
            if (req.getURI().getPath().endsWith(PROJECTS_URL)) {
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(projects));
            } else if (req.getURI().getPath().endsWith(PATCHES_URL)) {
                // request to fetch patches
                var query = req.getURI().getQuery();
                var parts = query.split("&");
                var state = parts[1].split("=")[1];
                var pps = patches.stream().filter(p -> p.state.status.equals(state)).toList();
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(pps));
            } else if (req.getURI().getPath().endsWith(ISSUES_URL)) {
                se = new StringEntity("[]");
            } else if (req.getURI().getPath().endsWith(PROJECTS_URL + "/" + patch.radProject.id + PATCHES_URL + "/" + patch.id)) {
                // request to re-fetch the patch
                var fetchedPatch = new RadPatch(patch);
                fetchedPatch.project = null;
                fetchedPatch.repo = null;
                fetchedPatch.seedNode = null;
                fetchedPatch.state = RadPatch.State.MERGED;
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(fetchedPatch));
            } else {
                // request to fetch specific project
                se = new StringEntity(RadicleProjectApi.MAPPER.writeValueAsString(projects.get(0)));
            }
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            final var resp = mock(CloseableHttpResponse.class);
            when(resp.getEntity()).thenReturn(se);
            final var statusLine = mock(StatusLine.class);
            when(resp.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(200);
            return resp;
        });

        radicleProjectSettingsHandler.saveRadHome(AbstractIT.RAD_HOME);
        radicleToolWindow = new RadicleToolWindow();
        var toolWindow = new MockToolWindow(super.getProject());
        radicleToolWindow.createToolWindowContent(super.getProject(), toolWindow);
        radicleToolWindow.toolWindowManagerListener.toolWindowShown(toolWindow);
        patchTabController = radicleToolWindow.patchTabController;
        patchTabController.createPatchProposalPanel(patch);
        var editorManager = FileEditorManager.getInstance(getProject());
        var allEditors = editorManager.getAllEditors();
        assertThat(allEditors.length).isEqualTo(1);
        var editor = allEditors[0];
        editorFile = editor.getFile();
        var providerManager = FileEditorProviderManager.getInstance();
        var providers = providerManager.getProviderList(getProject(), editorFile);
        assertThat(providers.size()).isEqualTo(1);
        patchEditorProvider = (PatchEditorProvider) providers.get(0);
        // Open createEditor
        patchEditorProvider.createEditor(getProject(), editorFile);
        //Wait to load the patch proposal
        Thread.sleep(100);
        executeUiTasks();
    }

    @Test
    public void testPatchInfoPanel() {
        var panel = patchTabController.getPatchProposalJPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var actionPanel = ef.getFirstComponent();
        var components = actionPanel.getComponents();
        var myPanels = (JPanel) components[0];
        var allPanels = myPanels.getComponents();

        // find the labels for branches, something like <default_branch> <- <patch_branch>
        var branchLabels = UIUtil.findComponentsOfType((JPanel) allPanels[0], JLabel.class);
        assertThat(branchLabels).hasSizeGreaterThanOrEqualTo(3);
        assertThat(branchLabels.get(0).getText()).isEqualTo(patch.radProject.defaultBranch);
        assertThat(branchLabels.get(2).getText()).contains(patch.id);

        // find patch title and id labels
        var patchTitleIdPanes = UIUtil.findComponentsOfType((JPanel) allPanels[1], BaseHtmlEditorPane.class);
        assertThat(patchTitleIdPanes).hasSizeGreaterThanOrEqualTo(2);
        assertThat(patchTitleIdPanes.get(0).getText()).contains(patch.title);
        assertThat(patchTitleIdPanes.get(1).getText()).contains(patch.id);

        // find view timeline link and checkout buttons
        var timelineCheckoutBtns = UIUtil.findComponentsOfType((JPanel) allPanels[2], JButton.class);
        assertThat(timelineCheckoutBtns).hasSizeGreaterThanOrEqualTo(2);
        assertThat(timelineCheckoutBtns.get(0).getText()).isEqualTo(RadicleBundle.message("view.timeline"));
        assertThat(timelineCheckoutBtns.get(1).getText()).isEqualTo(RadicleBundle.message("checkout"));

        var mergeBtn = UIUtil.findComponentOfType(ef.getSecondComponent(), JButton.class);
        assertThat(mergeBtn.getText()).isEqualTo(RadicleBundle.message("merge"));
    }

    @Test
    public void testMergePatch() throws Exception {
        addRadRemote(patch.project, patch.repo);
        var git = mock(Git.class);
        when(git.tip(any(), any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.runCommand((GitLineHandler) any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.merge(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.push(any(), any(), any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.runCommandWithoutCollectingOutput(any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.config(any(), any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), Git.class, git, getTestRootDisposable());

        var panel = patchTabController.getPatchProposalJPanel();
        var ef = UIUtil.findComponentOfType(panel, OnePixelSplitter.class);
        var mergeBtn = UIUtil.findComponentOfType(ef.getSecondComponent(), JButton.class);
        assertThat(mergeBtn).isNotNull();
        var mergePatchAction = (MergePatchAction) mergeBtn.getAction();
        assertThat(mergePatchAction).isNotNull();
        ApplicationManager.getApplication().executeOnPooledThread(() -> mergeBtn.doClick());

        while (mergePatchAction.performed == null) {
            executeUiTasks();
            Thread.sleep(10);
        }

        for (int i = 0; i < 100; i++) {
            executeUiTasks();
            try {
                mergePatchAction.performed.get(100, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) { }
        }
        assertThat(mergePatchAction.performed.isDone()).isTrue();
        assertThat(mergePatchAction.merged).isTrue();
    }
}
