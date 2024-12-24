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
import git4idea.commands.GitLineHandlerListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.AbstractIT;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadProject;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.timeline.editor.PatchEditorProvider;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.RadicleToolWindow;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static network.radicle.jetbrains.radiclejetbrainsplugin.GitTestUtil.addRadRemote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class PatchProposalPanelTest extends AbstractIT {
    private static final Logger logger = Logger.getInstance(MergePatchAction.class);

    RadicleToolWindow radicleToolWindow;
    List<RadPatch> patches;
    List<RadProject> projects;
    RadPatch patch;
    PatchTabController patchTabController;
    VirtualFile editorFile;
    PatchEditorProvider patchEditorProvider;

    @Before
    public void setUpToolWindow() throws InterruptedException {
        projects = PatchListPanelTest.getTestProjects();
        patches = PatchListPanelTest.getTestPatches();
        patch = new RadPatch(patches.getFirst());
        patch.repo = firstRepo;
        patch.project = getProject();
        patch.radProject.defaultBranch = projects.getFirst().defaultBranch;

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
        patchEditorProvider = (PatchEditorProvider) providers.getFirst();
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
        assertThat(timelineCheckoutBtns.get(0).getText()).isEqualTo(RadicleBundle.message("open.in.editor"));
        assertThat(timelineCheckoutBtns.get(1).getText()).isEqualTo(RadicleBundle.message("checkout"));

        var mergeBtn = UIUtil.findComponentOfType(ef.getSecondComponent(), JButton.class);
        assertThat(mergeBtn.getText()).isEqualTo(RadicleBundle.message("merge"));
    }

    @Test
    public void testMergePatch() throws Exception {
        addRadRemote(patch.repo);
        var git = mock(Git.class);
        when(git.tip(any(), any())).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
        when(git.runCommand((GitLineHandler) any())).thenAnswer(i -> {
            GitLineHandler glh = i.getArgument(0);
            var pcl = glh.printableCommandLine();
            List<String> out = List.of();
            if (pcl.contains("--show-current")) {
                out = List.of("main");
            }
            return new GitCommandResult(false, 0, List.of(), out);
        });
        when(git.merge(any(), any(), any(), any(GitLineHandlerListener[].class))).thenReturn(new GitCommandResult(false, 0, List.of(), List.of()));
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
        var md = Mockito.mockingDetails(git);
        logger.warn("Git mocking details: " + md.getInvocations());
        verify(git).merge(any(), any(), any(), any(GitLineHandlerListener[].class));
        assertThat(mergePatchAction.merged).isTrue();
    }
}
