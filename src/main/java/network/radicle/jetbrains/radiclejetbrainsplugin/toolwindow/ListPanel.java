package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListUiUtil;
import com.intellij.vcs.log.ui.frame.ProgressStripe;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadInspect;
import network.radicle.jetbrains.radiclejetbrainsplugin.config.RadicleProjectSettingsHandler;
import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleProjectApi;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static kotlinx.coroutines.CoroutineScopeKt.MainScope;

public abstract class ListPanel<P, Q extends ReviewListSearchValue, S extends SearchViewModelBase<Q, ?, P>> {
    private static final Logger logger = Logger.getInstance(ListPanel.class);
    protected final TabController<P, Q, S> controller;
    protected final Project project;
    protected final RadicleProjectApi api;
    protected final RadicleProjectSettingsHandler radicleProjectSettingsHandler;
    protected ProgressStripe progressStripe;
    protected List<P> loadedData = null;
    protected JBList<P> list;
    protected DefaultListModel<P> model;
    protected S searchVm;

    public ListPanel(TabController<P, Q, S> controller, Project project) {
        this.controller = controller;
        this.project = project;
        this.api = project.getService(RadicleProjectApi.class);
        this.radicleProjectSettingsHandler = new RadicleProjectSettingsHandler(project);
        this.model = new DefaultListModel<>();
    }

    public JComponent create() {
        var scope = MainScope();
        Disposer.register(controller.getDisposer(), () -> CoroutineScopeKt.cancel(scope, null));
        searchVm = getViewModel(scope);
        var filterPanel = getFilterPanel(searchVm, scope);
        var verticalPanel = new JPanel(new VerticalLayout(5));
        verticalPanel.add(filterPanel);
        var mainPanel = JBUI.Panels.simplePanel();
        var listPanel = createListPanel();
        mainPanel.addToTop(verticalPanel);
        mainPanel.addToCenter(listPanel);
        updateListPanel();
        searchVm.getSearchState().collect((patchListSearchValue, continuation) -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
               try {
                   var isDataLoaded = searchVm.getCountDown().await(5, TimeUnit.SECONDS);
                   ApplicationManager.getApplication().invokeLater(() -> {
                       if (isDataLoaded) {
                           filterList(patchListSearchValue);
                           updateListEmptyText(patchListSearchValue);
                       }
                   }, ModalityState.any());
               } catch (Exception e) {
                   logger.warn("Unable to wait for data", e);
               }
            });
            return null;
        }, new Continuation<>() {
            @Override
            public void resumeWith(@NotNull Object o) {

            }

            @NotNull
            @Override
            public CoroutineContext getContext() {
                return scope.getCoroutineContext();
            }
        });
        return mainPanel;
    }

    public void updateListPanel() {
        var countDown = new CountDownLatch(1);
        if (searchVm != null) {
            searchVm.setCountDown(countDown);
        }
        var settings =  radicleProjectSettingsHandler.loadSettings();
        var seedNode = settings.getSeedNode();
        if (seedNode == null || Strings.isNullOrEmpty(seedNode.url)) {
            return;
        }
        var gitRepoManager = GitRepositoryManager.getInstance(project);
        var repos = gitRepoManager.getRepositories();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            model.clear();
            progressStripe.startLoading();
            loadedData = getData(repos);
            searchVm.setList(loadedData);
            this.model.addAll(loadedData);
            countDown.countDown();
            ApplicationManager.getApplication().invokeLater(() -> {
                progressStripe.stopLoading();
                updateListEmptyText(searchVm.getValue());
            }, ModalityState.any());
        });
    }

    private JPanel createListPanel() {
        list = new JBList<>(this.model);
        list.setCellRenderer(this.getCellRenderer());
        list.setExpandableItemsEnabled(false);
        list.getEmptyText().clear();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ScrollingUtil.installActions(list);
        ListUtil.installAutoSelectOnMouseMove(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnFocus(list);
        ListUiUtil.Selection.INSTANCE.installSelectionOnRightClick(list);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != 1 || e.getClickCount() != 2) {
                    return;
                }
                final var selected = list.getSelectedValue();
                model.clear();
                onItemClick(selected);
            }
        });
        var scrollPane = ScrollPaneFactory.createScrollPane(list, true);
        progressStripe = new ProgressStripe(scrollPane, controller.getDisposer(), ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
        return progressStripe;
    }

    public void updateListEmptyText(ReviewListSearchValue patchListSearchValue) {
        if (this.loadedData == null) {
            return;
        }
        list.getEmptyText().clear();
        if (loadedData.isEmpty() || model.isEmpty()) {
            list.getEmptyText().setText(RadicleBundle.message("nothingFound"));
        }
        if (patchListSearchValue.getFilterCount() > 0 && (loadedData.isEmpty() || model.isEmpty())) {
            list.getEmptyText().appendSecondaryText(RadicleBundle.message("clearFilters"), SimpleTextAttributes.LINK_ATTRIBUTES,
                    e -> resetFilters());
        }
    }
    private List<P> getData(List<GitRepository> repos) {
        var outputs = new ArrayList<P>();
        var radInitializedRepos = RadAction.getInitializedReposWithNodeConfigured(repos, true);
        if (radInitializedRepos.isEmpty()) {
            return List.of();
        }
        for (GitRepository repo : radInitializedRepos) {
            var radInspect = new RadInspect(repo);
            var output = radInspect.perform();
            if (RadAction.isSuccess(output)) {
                var radProjectId = output.getStdout().trim();
                var data = fetchData(radProjectId, repo);
                if (!data.isEmpty()) {
                    outputs.addAll(data);
                }
            }
        }
        return outputs;
    }

    public abstract List<P> fetchData(String projectId, GitRepository repo);

    public abstract ListCellRenderer<P> getCellRenderer();

    public abstract S getViewModel(CoroutineScope scope);

    public abstract void onItemClick(P obj);

    public abstract JComponent getFilterPanel(S searchViewModel, CoroutineScope scope);

    public abstract void filterList(Q searchValue);

    public abstract Q getEmptySearchValueModel();

    public DefaultListModel<P> getModel() {
        return model;
    }

    public S getSearchVm() {
        return searchVm;
    }

    public JBList<P> getList() {
        return list;
    }

    private void resetFilters() {
        searchVm.getSearchState().setValue(getEmptySearchValueModel());
    }
}
