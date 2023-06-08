package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepositoryManager;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SearchViewModelBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class IssueSearchPanelViewModel extends SearchViewModelBase<IssueListSearchValue, IssueSearchPanelViewModel.IssueListQuickFilter, RadIssue> {
    private static final Logger logger = LoggerFactory.getLogger(IssueSearchPanelViewModel.class);

    private final Project project;
    private List<String> projectNames = List.of();
    public IssueSearchPanelViewModel(@NotNull CoroutineScope scope,
                                     @NotNull ReviewListSearchHistoryModel<IssueListSearchValue> historyModel,
                                     Project project) {
        super(scope, historyModel, new IssueListSearchValue(), new IssueListQuickFilter());
        this.project = project;
    }

    public MutableStateFlow<String> authorFilterState() {
        return partialState(getSearchState(), IssueListSearchValue::getAuthor,
                (Function2<IssueListSearchValue, Object, IssueListSearchValue>) (issueListSearchValue, authorName) -> {
                    var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
                    copyIssueSearchValue.author = (String) authorName;
                    return copyIssueSearchValue;
                });
    }

    public MutableStateFlow<String> projectFilterState() {
        return partialState(getSearchState(), IssueListSearchValue::getProject,
                (Function2<IssueListSearchValue, Object, IssueListSearchValue>) (issueListSearchValue, projectName) -> {
                    var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
                    copyIssueSearchValue.project = (String) projectName;
                    return copyIssueSearchValue;
                });
    }

    public MutableStateFlow<String> stateFilter() {
        return partialState(getSearchState(), IssueListSearchValue::getState,
                (Function2<IssueListSearchValue, Object, IssueListSearchValue>) (issueListSearchValue, state) -> {
                    var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
                    copyIssueSearchValue.state = (String) state;
                    return copyIssueSearchValue;
                });
    }

    public MutableStateFlow<String> tagFilter() {
        return partialState(getSearchState(), IssueListSearchValue::getTag,
                (Function2<IssueListSearchValue, Object, IssueListSearchValue>) (issueListSearchValue, tag) -> {
                    var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
                    copyIssueSearchValue.tag = (String) tag;
                    return copyIssueSearchValue;
                });
    }

    public List<String> getProjectNames() {
        var isFinished = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            projectNames = RadAction.getInitializedReposWithNodeConfigured(gitRepoManager.getRepositories(), true)
                    .stream().map(e -> e.getRoot().getName()).collect(Collectors.toList());
            isFinished.countDown();
        });
        try {
            isFinished.await();
        } catch (Exception e) {
            logger.warn("Unable to get project names", e);
        }
        return projectNames;
    }

    public List<String> getTags() {
        var selectedProjectFilter = this.getSearchState().getValue().project;
        List<String> tags = new ArrayList<>();
        try {
            countDown.await();
        } catch (Exception e) {
            logger.warn("Unable to get rad tags", e);
            return tags;
        }
        for (var p : myList) {
            if (selectedProjectFilter != null && !p.repo.getRoot().getName().equals(selectedProjectFilter)) {
                continue;
            }
            for (var tag : p.tags) {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    public List<String> getAuthors() {
        var selectedProjectFilter = this.getSearchState().getValue().project;
        List<String> peersIds = new ArrayList<>();
        try {
            countDown.await();
        } catch (Exception e) {
            logger.warn("Unable to get rad patches", e);
            return peersIds;
        }
        for (var p : myList) {
            if (selectedProjectFilter != null && !p.repo.getRoot().getName().equals(selectedProjectFilter)) {
                continue;
            }
            if (!peersIds.contains(p.author.id())) {
                peersIds.add(p.author.id());
            }
        }
        return peersIds;
    }

    @NotNull
    @Override
    public List<IssueListQuickFilter> getQuickFilters() {
        var openFilter = new IssueSearchPanelViewModel.IssueListQuickFilter();
        openFilter.issueListSearchValue.state = RadIssue.State.OPEN.status;

        var closedFilter = new IssueSearchPanelViewModel.IssueListQuickFilter();
        closedFilter.issueListSearchValue.state = RadIssue.State.CLOSED.status;

        return List.of(openFilter, closedFilter);
    }

    @NotNull
    @Override
    protected IssueListSearchValue withQuery(@NotNull IssueListSearchValue issueListSearchValue, @Nullable String searchStr) {
        var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
        copyIssueSearchValue.searchQuery = searchStr;
        return copyIssueSearchValue;
    }

    public static class IssueListQuickFilter implements ReviewListQuickFilter<IssueListSearchValue> {
        private final IssueListSearchValue issueListSearchValue;
        public IssueListQuickFilter() {
            issueListSearchValue = new IssueListSearchValue();
            // Set OPEN as default filter
            issueListSearchValue.state = RadIssue.State.OPEN.status;
        }

        @NotNull
        @Override
        public IssueListSearchValue getFilter() {
            return issueListSearchValue;
        }
    }
}
