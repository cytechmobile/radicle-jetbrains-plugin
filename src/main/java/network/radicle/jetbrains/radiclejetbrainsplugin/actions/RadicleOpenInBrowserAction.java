package network.radicle.jetbrains.radiclejetbrainsplugin.actions;

import com.google.common.base.Strings;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

import static network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction.isProjectRadInitialized;

public class RadicleOpenInBrowserAction extends AnAction {
    public static final String UI_URL = "https://app.radicle.xyz/nodes/";
    private GitRepository myRepo = null;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        var project = e.getProject();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        var selectedFileToOpen = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (selectedFileToOpen == null || selectedFileToOpen.isDirectory()) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        myRepo = GitUtil.getRepositoryManager(e.getProject()).getRepositoryForFileQuick(selectedFileToOpen);
        if (myRepo == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        var isRadInitialized = isProjectRadInitialized(myRepo);
        if (!isRadInitialized) {
            presentation.setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        var selectedFileToOpen = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (selectedFileToOpen == null || myRepo == null) {
            return;
        }
        var fileToOpen = selectedFileToOpen.toString().replace(myRepo.getRoot().toString(), "").trim();
        var editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            var caretModel = editor.getCaretModel().getOffset();
            var lineNumber = editor.getDocument().getLineNumber(caretModel) + 1;
            fileToOpen += "#L" + lineNumber;
        }
        openInBrowser(e.getProject(), myRepo, fileToOpen, BrowserLauncher.getInstance());
    }

    public void openInBrowser(Project project, GitRepository repo, String fileToOpen, BrowserLauncher browserLauncher) {
        var radicleSettingsHandler = new RadicleProjectSettingsHandler(project);
        var nodeUrl = radicleSettingsHandler.loadSettings().getSeedNode().url
                .replace("http://", "").replace("https://", "");
        if (Strings.isNullOrEmpty(nodeUrl)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var radInspect = new RadInspect(repo);
            var output = radInspect.perform();
            if (!RadAction.isSuccess(output)) {
                return;
            }
            var radProjectId = output.getStdout().trim();
            var url = UI_URL + nodeUrl + "/" + radProjectId + "/tree" + fileToOpen;
            browserLauncher.browse(URI.create(url));
        });
    }
}
