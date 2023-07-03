package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepositoryManager;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadAuthor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public abstract class SearchViewModelBase<T extends ReviewListSearchValue, E extends ReviewListQuickFilter<T>, Q>
        extends ReviewListSearchPanelViewModelBase<T, E> {
    protected final Project project;
    protected CountDownLatch countDown;
    protected List<Q> myList;
    private List<String> projectNames = List.of();

    public SearchViewModelBase(@NotNull CoroutineScope scope, @NotNull ReviewListSearchHistoryModel<T> historyModel,
                               @NotNull T emptySearch, @NotNull E defaultQuickFilter, Project project) {
        super(scope, historyModel, emptySearch, defaultQuickFilter);
        this.project = project;
    }

    public void setList(List<Q> list) {
        this.myList = list;
    }

    public void setCountDown(CountDownLatch countdown) {
        this.countDown = countdown;
    }

    public CountDownLatch getCountDown() {
        return countDown;
    }

    public ReviewListSearchValue getValue() {
        return this.getSearchState().getValue();
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
            var selectedProjectFilter = getSelectedProjectFilter();
            for (var el : myList) {
                if (selectedProjectFilter != null && isProjectFilterMatch(el, selectedProjectFilter)) {
                    continue;
                }
                List<String> itemTags = getItemTags(el);
                for (var tag : itemTags) {
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
            return tags;
        });
    }

    public CompletableFuture<List<String>> getAuthors() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> peersIds = new ArrayList<>();
            var selectedProjectFilter = getSelectedProjectFilter();
            for (var el : myList) {
                if (selectedProjectFilter != null && isProjectFilterMatch(el, selectedProjectFilter)) {
                    continue;
                }
                var author = getAuthor(el);
                if (!peersIds.contains(author.id)) {
                    peersIds.add(author.id);
                }
            }
            return peersIds;
        });
    }

    protected abstract RadAuthor getAuthor(Q item);

    protected abstract String getSelectedProjectFilter();

    protected abstract List<String> getItemTags(Q item);

    protected abstract boolean isProjectFilterMatch(Q item, String projectFilter);

}
