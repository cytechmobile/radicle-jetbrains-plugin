package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

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
import git4idea.repo.GitRepositoryChangeListener;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.issues.IssueTabController;
import network.radicle.jetbrains.radiclejetbrainsplugin.patches.PatchTabController;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

public class RadicleToolWindow extends VcsToolWindowFactory {
    public ToolWindowManagerListener toolWindowManagerListener;
    public PatchTabController patchTabController;
    public IssueTabController issueTabController;
    public ContentManager contentManager;
    public AnAction newIssueAction;
    private Content issueContent;
    private Content patchContent;

    @Override
    public void init(@NotNull ToolWindow window) {
        super.init(window);
        //Workaround at activating toolwindow content and check for available radicle repos
        window.getContentManager();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
        contentManager = toolWindow.getContentManager();
        issueContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);
        patchContent = toolWindow.getContentManager().getFactory().createContent(new JPanel(null), null, false);

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
                    issueTabController.createPanel();
                    contentManager.addContentManagerListener(new ContentManagerListener() {
                        @Override
                        public void selectionChanged(@NotNull ContentManagerEvent event) {
                            setUpActions(toolWindow);
                        }
                    });
                    setUpActions(toolWindow);
                }
            }
        };

        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, toolWindowManagerListener);
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE,
                (GitRepositoryChangeListener) repository -> {
                    //var gitRepoManager = GitRepositoryManager.getInstance(project);
                    //var repos = gitRepoManager.getRepositories();
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInited = RadAction.getInitializedReposWithNodeConfigured(List.of(repository), false);
                        if (!radInited.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                toolWindow.setAvailable(true);
                            });
                        }
                    });
                    /*ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, false);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!radInitializedRepos.isEmpty()) {
                                toolWindow.setAvailable(true);
                            }
                        });
                    });*/
                });
    }

    private void setUpActions(ToolWindow toolWindow) {
        var taskBarActions = new ArrayList<AnAction>();
        taskBarActions.add(new AnAction(AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (contentManager.isSelected(patchContent)) {
                    patchTabController.createPanel();
                } else if (contentManager.isSelected(issueContent)) {
                    issueTabController.createPanel();
                }
            }
        });

        if (contentManager.isSelected(issueContent)) {
            newIssueAction = new AnAction(AllIcons.General.Add) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    issueTabController.createNewIssuePanel();
                }
            };
            taskBarActions.add(newIssueAction);
        }
        toolWindow.setTitleActions(taskBarActions);
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return false;
    }
}
