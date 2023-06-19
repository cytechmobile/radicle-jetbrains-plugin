package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.openapi.project.Project;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.SearchViewModelBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IssueSearchPanelViewModel extends SearchViewModelBase<IssueListSearchValue, IssueSearchPanelViewModel.IssueListQuickFilter, RadIssue> {
    private final Project project;
    public IssueSearchPanelViewModel(@NotNull CoroutineScope scope,
                                     @NotNull ReviewListSearchHistoryModel<IssueListSearchValue> historyModel,
                                     Project project) {
        super(scope, historyModel, new IssueListSearchValue(), new IssueListQuickFilter());
        this.project = project;
    }

    @NotNull
    @Override
    public List<IssueListQuickFilter> getQuickFilters() {
        return List.of();
    }

    @NotNull
    @Override
    protected IssueListSearchValue withQuery(@NotNull IssueListSearchValue issueListSearchValue, @Nullable String s1) {
        return new IssueListSearchValue();
    }

    public static class IssueListQuickFilter implements ReviewListQuickFilter<IssueListSearchValue> {
        private final IssueListSearchValue issueListSearchValue;
        public IssueListQuickFilter() {
            issueListSearchValue = new IssueListSearchValue();
        }
        @NotNull
        @Override
        public IssueListSearchValue getFilter() {
            return issueListSearchValue;
        }
    }
}
