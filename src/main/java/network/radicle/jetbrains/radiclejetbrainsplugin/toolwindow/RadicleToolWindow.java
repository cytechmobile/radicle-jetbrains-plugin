package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.dvcs.repo.VcsRepositoryMappingListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueTabController;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchTabController;
import org.jetbrains.annotations.NotNull;
import javax.swing.JPanel;
import java.util.List;
import static com.intellij.dvcs.repo.VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED;

public class RadicleToolWindow extends VcsToolWindowFactory {
    public static final String TOOL_WINDOW_NAME = "Radicle";
    public static final Key<List<GitRepository>> RAD_REPOS_KEY = Key.create("RAD_REPOS");
    public static final Key<Boolean> ISSUE_LIST_INITIALIZED_KEY = Key.create("ISSUE_LIST_INITIALIZED");
    public ToolWindow myToolWindow;
    public static String id;
    public ToolWindowManagerListener toolWindowManagerListener;
    public PatchTabController patchTabController;
    public IssueTabController issueTabController;
    public enum Action {
        ADD, REFRESH
    }

    @Override
    public void init(@NotNull ToolWindow window) {
        super.init(window);
        id = window.getId();
        var connection = window.getProject().getMessageBus().connect(window.getDisposable());
        connection.subscribe(VCS_REPOSITORY_MAPPING_UPDATED, (VcsRepositoryMappingListener) () -> {
            var gitRepoManager = GitRepositoryManager.getInstance(window.getProject());
            var repos = gitRepoManager.getRepositories();
            var radInited = RadAction.getInitializedReposWithNodeConfigured(repos, false);
            if (!radInited.isEmpty()) {
                ClientProperty.put(window.getComponent(), RAD_REPOS_KEY, radInited);
                ApplicationManager.getApplication().invokeLater(() -> window.setAvailable(true));
            }
        });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        myToolWindow = toolWindow;
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        toolWindowManagerListener = new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                if (toolWindow == shownToolWindow && toolWindow.isVisible() && toolWindow.getContentManager().isEmpty()) {
                    var contentManager = toolWindow.getContentManager();
                    var issueContent = contentManager.getFactory().createContent(new JPanel(null), RadicleBundle.message("issues"), false);
                    var patchContent = contentManager.getFactory().createContent(new JPanel(null), RadicleBundle.message("patches"), false);
                    patchContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadiclePatchProposalsContent"));
                    issueContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadicleIssueContent"));
                    contentManager.addContent(patchContent);
                    contentManager.addContent(issueContent);
                    patchTabController = new PatchTabController(patchContent, project);
                    patchTabController.createPanel();
                    var toolbarActions = List.of(new AddAction(), new RefreshAction());
                    toolWindow.setTitleActions(toolbarActions);
                    contentManager.addContentManagerListener(new ContentListener(toolWindow));
                }
            }
        };
        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, toolWindowManagerListener);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
       return false;
    }

    public ContentManager getContentManager() {
        return myToolWindow.getContentManager();
    }

    public class ContentListener implements ContentManagerListener {
        private final ToolWindow toolWindow;

        public ContentListener(ToolWindow toolWindow) {
            this.toolWindow = toolWindow;
        }

        @Override
        public void selectionChanged(@NotNull ContentManagerEvent event) {
            var contentManager = toolWindow.getContentManager();
            var issueContent = contentManager.getContent(1);
            if (issueContent == null) {
                return;
            }
            var myInitialized = ClientProperty.get(toolWindow.getComponent(), ISSUE_LIST_INITIALIZED_KEY);
            if (contentManager.isSelected(issueContent) && myInitialized == null) {
                issueTabController = new IssueTabController(issueContent, toolWindow.getProject());
                issueTabController.createPanel();
                ClientProperty.put(toolWindow.getComponent(), ISSUE_LIST_INITIALIZED_KEY, true);
            }
        }
    }

    private static void handleAction(@NotNull AnActionEvent e, Action action) {
        if (e.getProject() == null) {
            return;
        }
        var toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow(TOOL_WINDOW_NAME);
        if (toolWindow == null) {
            return;
        }
        var contentManager = toolWindow.getContentManager();
        var patchContent = contentManager.getContent(0);
        var issueContent = contentManager.getContent(1);
        if (patchContent == null || issueContent == null) {
            return;
        }
        // Patch tab selected
        if (contentManager.isSelected(patchContent)) {
            var patchTabController = new PatchTabController(patchContent, e.getProject());
            if (action == Action.ADD) {
                var repos = ClientProperty.get(toolWindow.getComponent(), RAD_REPOS_KEY);
                patchTabController.createNewPatchPanel(repos);
            } else if (action == Action.REFRESH) {
                patchTabController.createPanel();
            }
        }
        // Issue tab selected
        if (contentManager.isSelected(issueContent)) {
            var issueTabController = new IssueTabController(issueContent, e.getProject());
            if (action == Action.ADD) {
                issueTabController.createNewIssuePanel();
            } else if (action == Action.REFRESH) {
                issueTabController.createPanel();
            }
        }
    }

    private static class AddAction extends DumbAwareAction {
        public AddAction() {
            super(AllIcons.General.Add);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            handleAction(e, Action.ADD);
        }
    }

    private static class RefreshAction extends DumbAwareAction {
        public RefreshAction() {
            super(AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            handleAction(e, Action.REFRESH);
        }
    }
}
