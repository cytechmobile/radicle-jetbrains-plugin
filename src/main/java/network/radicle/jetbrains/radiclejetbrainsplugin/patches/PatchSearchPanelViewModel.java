package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepositoryManager;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import network.radicle.jetbrains.radiclejetbrainsplugin.actions.rad.RadAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class PatchSearchPanelViewModel extends ReviewListSearchPanelViewModelBase<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilter> implements ReviewListSearchPanelViewModel<PatchListSearchValue, PatchSearchPanelViewModel.PatchListQuickFilter> {

    private static final Logger logger = LoggerFactory.getLogger(PatchSearchPanelViewModel.class);

    private Project project;
    private List<String> projectNames = List.of();

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
            newPatchSearchValue.project = patchListSearchValue.project;
            return newPatchSearchValue;
        });
    }

    public MutableStateFlow<String> projectFilterState() {
        return partialState(getSearchState(), PatchListSearchValue::getProject,
                (Function2<PatchListSearchValue, Object, PatchListSearchValue>) (patchListSearchValue, projectName) -> {
                    var newPatchSearchValue = new PatchListSearchValue();
                    newPatchSearchValue.state = patchListSearchValue.state;
                    newPatchSearchValue.searchQuery = patchListSearchValue.searchQuery;
                    newPatchSearchValue.project = (String) projectName;
                    return newPatchSearchValue;
                });
    }

    public List<String> getProjectNames()  {
        var isFinished = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var gitRepoManager = GitRepositoryManager.getInstance(project);
            projectNames = RadAction.getInitializedReposWithNodeConfigured(gitRepoManager.getRepositories(), true)
                    .stream().map(e -> e.getProject().getName()).collect(Collectors.toList());
            isFinished.countDown();
        });
        try {
            isFinished.await();
        } catch (Exception e) {
            logger.warn("Unable to get project names");
        }
        return projectNames;
    }

    @NotNull
    @Override
    protected PatchListSearchValue withQuery(@NotNull PatchListSearchValue patchListSearchValue, @Nullable String searchStr) {
        var newPatchSearchValue = new PatchListSearchValue();
        newPatchSearchValue.searchQuery = searchStr;
        newPatchSearchValue.state = patchListSearchValue.state;
        newPatchSearchValue.project = patchListSearchValue.project;
        return newPatchSearchValue;
    }

    @Override
    public List<PatchListQuickFilter> getQuickFilters() {
        var stateQuickFilter = new PatchListQuickFilter().openStateQuickFilter();
        return List.of(stateQuickFilter);
    }


    public static class PatchListQuickFilter implements ReviewListQuickFilter<PatchListSearchValue> {

        private PatchListSearchValue patchListSearchValue;

        public PatchListQuickFilter() {
            patchListSearchValue = new PatchListSearchValue();
        }

        public PatchListQuickFilter openStateQuickFilter() {
            patchListSearchValue = new PatchListSearchValue();
            patchListSearchValue.state = PatchListSearchValue.State.OPEN.name;
            return this;
        }

        @NotNull
        @Override
        public PatchListSearchValue getFilter() {
            return patchListSearchValue;
        }
    }

}
