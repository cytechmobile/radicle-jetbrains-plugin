package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchSearchPanelViewModel extends ReviewListSearchPanelViewModelBase<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilter> {

    private Project project;

    public PatchSearchPanelViewModel(@NotNull CoroutineScope scope,
                                     @NotNull ReviewListSearchHistoryModel<PatchListSearchValue> historyModel,Project project) {
        super(scope, historyModel, new PatchListSearchValue(), new PatchListQuickFilter());
        this.project = project;
    }

    public MutableStateFlow<String> stateFilterState() {
       return partialState(getSearchState(), PatchListSearchValue::getState,
               (Function2<PatchListSearchValue, Object, PatchListSearchValue>) (patchListSearchValue, newState) -> {
            var newPatchSearchValue = new PatchListSearchValue();
            newPatchSearchValue.state = (String) newState;
            newPatchSearchValue.searchQuery = patchListSearchValue.searchQuery;
            return newPatchSearchValue;
        });
    }

    @NotNull
    @Override
    public List<PatchListQuickFilter> getQuickFilters() {
        return List.of();
    }

    @NotNull
    @Override
    protected PatchListSearchValue withQuery(@NotNull PatchListSearchValue patchListSearchValue, @Nullable String s1) {
        var newPatchSearchValue = new PatchListSearchValue();
        newPatchSearchValue.searchQuery = s1;
        newPatchSearchValue.state = patchListSearchValue.state;
        return newPatchSearchValue;
    }

    public static class PatchListQuickFilter implements ReviewListQuickFilter<PatchListSearchValue> {

        @NotNull
        @Override
        public PatchListSearchValue getFilter() {
            return new PatchListSearchValue();
        }
    }
}
