package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.dvcs.repo.VcsRepositoryMappingListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.changes.ui.VcsToolWindowFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.ui.content.Content;
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
import java.util.ArrayList;
import java.util.List;
import static com.intellij.dvcs.repo.VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED;

public class RadicleToolWindow extends VcsToolWindowFactory {
    public static String id;
    private List<GitRepository> gitRepos = new ArrayList<>();
    public ToolWindowManagerListener toolWindowManagerListener;
    public PatchTabController patchTabController;
    public IssueTabController issueTabController;
    public ContentManager contentManager;
    private Content issueContent;
    private Content patchContent;
    private boolean isListInitialized = false;
    private boolean isEnable = false;

    @Override
    public void init(@NotNull ToolWindow window) {
        super.init(window);
        id = window.getId();
        //Workaround at activating toolwindow content and check for available radicle repos
        window.getContentManager();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        contentManager = toolWindow.getContentManager();
        issueContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), RadicleBundle.message("issues"), false);
        patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), RadicleBundle.message("patches"), false);

        patchContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadiclePatchProposalsContent"));
        issueContent.setDisposer(Disposer.newDisposable(toolWindow.getDisposable(), "RadicleIssueContent"));
        toolWindowManagerListener = new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow shownToolWindow) {
                if (toolWindow == shownToolWindow && toolWindow.isVisible() && contentManager.isEmpty()) {
                    contentManager.addContent(patchContent);
                    contentManager.addContent(issueContent);
                    patchTabController = new PatchTabController(patchContent, project);
                    patchTabController.createPanel();
                    issueTabController = new IssueTabController(issueContent, project);
                    toolWindow.setTitleActions(List.of(new AnAction(AllIcons.General.Add) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            if (contentManager.isSelected(patchContent)) {
                                patchTabController.createNewPatchPanel(gitRepos);
                            } else if (contentManager.isSelected(issueContent)) {
                                issueTabController.createNewIssuePanel();
                            }
                        }
                    }, new AnAction(AllIcons.Actions.Refresh) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            if (contentManager.isSelected(patchContent)) {
                                patchTabController.createPanel();
                            } else if (contentManager.isSelected(issueContent)) {
                                issueTabController.createPanel();
                            }
                        }
                    }));
                    contentManager.addContentManagerListener(new ContentManagerListener() {
                        @Override
                        public void selectionChanged(@NotNull ContentManagerEvent event) {
                            if (contentManager.isSelected(issueContent) && issueTabController != null && !isListInitialized) {
                                issueTabController.createPanel();
                                isListInitialized = true;
                            }
                        }
                    });
                }
            }
        };

        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, toolWindowManagerListener);
        project.getMessageBus().connect().subscribe(VCS_REPOSITORY_MAPPING_UPDATED, (VcsRepositoryMappingListener) () -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            var repos = gitRepoManager.getRepositories();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var radInited = RadAction.getInitializedReposWithNodeConfigured(repos, false);
                gitRepos = radInited;
                if (!radInited.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        toolWindow.setAvailable(true);
                        isEnable = true;
                    });
                }
            });
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return isEnable;
    }
}
