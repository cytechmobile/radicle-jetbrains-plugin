package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
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
import java.util.concurrent.CompletableFuture;
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

    public MutableStateFlow<String> assigneeFilter() {
        return partialState(getSearchState(), IssueListSearchValue::getAssignee,
                (Function2<IssueListSearchValue, Object, IssueListSearchValue>) (issueListSearchValue, assignee) -> {
                    var copyIssueSearchValue = new IssueListSearchValue(issueListSearchValue);
                    copyIssueSearchValue.assignee = (String) assignee;
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

    public CompletableFuture<List<String>> getProjectNames() {
        return CompletableFuture.supplyAsync(() -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            projectNames = RadAction.getInitializedReposWithNodeConfigured(gitRepoManager.getRepositories(), true)
                    .stream().map(e -> e.getRoot().getName()).collect(Collectors.toList());
            return projectNames;
        });
    }

    public CompletableFuture<List<String>> getTags() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> tags = new ArrayList<>();
            var selectedProjectFilter = this.getSearchState().getValue().project;
            for (var issue : myList) {
                if (selectedProjectFilter != null && !issue.repo.getRoot().getName().equals(selectedProjectFilter)) {
                    continue;
                }
                for (var tag : issue.tags) {
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
            return tags;
        });
    }

    public CompletableFuture<List<String>> getAssignees() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> assigness = new ArrayList<>();
            var selectedProjectFilter = this.getSearchState().getValue().project;
            for (var issue : myList) {
                if (selectedProjectFilter != null && !issue.repo.getRoot().getName().equals(selectedProjectFilter)) {
                    continue;
                }
                for (var assignee : issue.assignees) {
                    if (!assigness.contains((String) assignee)) {
                        assigness.add((String) assignee);
                    }
                }
            }
            return assigness;
        });
    }

    public CompletableFuture<List<String>> getAuthors() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> peersIds = new ArrayList<>();
            var selectedProjectFilter = this.getSearchState().getValue().project;
            for (var issue : myList) {
                if (selectedProjectFilter != null && !issue.repo.getRoot().getName().equals(selectedProjectFilter)) {
                    continue;
                }
                if (!peersIds.contains(issue.author.id)) {
                    peersIds.add(issue.author.id);
                }
            }
            return peersIds;
        });
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
