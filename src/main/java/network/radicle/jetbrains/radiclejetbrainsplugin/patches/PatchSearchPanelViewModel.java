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
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class PatchSearchPanelViewModel
        extends ReviewListSearchPanelViewModelBase<PatchListSearchValue, PatchSearchPanelViewModel.PatchListQuickFilter>
        implements ReviewListSearchPanelViewModel<PatchListSearchValue, PatchSearchPanelViewModel.PatchListQuickFilter> {

    private static final Logger logger = LoggerFactory.getLogger(PatchSearchPanelViewModel.class);

    private final Project project;
    private List<String> projectNames = List.of();
    private List<RadPatch> radPatches;
    private CountDownLatch radPatchesCountDown;

    public PatchSearchPanelViewModel(@NotNull CoroutineScope scope,
                                     @NotNull ReviewListSearchHistoryModel<PatchListSearchValue> historyModel, Project project) {
        super(scope, historyModel, new PatchListSearchValue(), new PatchListQuickFilter());
        this.project = project;
    }

    public void setRadPatches(List<RadPatch> patches) {
        this.radPatches = patches;
    }

    public void setRadPatchesCountDown(CountDownLatch countdown) {
        this.radPatchesCountDown = countdown;
    }

    public MutableStateFlow<String> peerIdFilterState() {
        return partialState(getSearchState(), PatchListSearchValue::getPeerId,
                (Function2<PatchListSearchValue, Object, PatchListSearchValue>) (patchListSearchValue, authorName) -> {
                    var copyPatchSearchValue = new PatchListSearchValue(patchListSearchValue);
                    copyPatchSearchValue.peerId = (String) authorName;
                    return copyPatchSearchValue;
                });
    }

    public MutableStateFlow<String> projectFilterState() {
        return partialState(getSearchState(), PatchListSearchValue::getProject,
                (Function2<PatchListSearchValue, Object, PatchListSearchValue>) (patchListSearchValue, projectName) -> {
                    var copyPatchSearchValue = new PatchListSearchValue(patchListSearchValue);
                    copyPatchSearchValue.project = (String) projectName;
                    return copyPatchSearchValue;
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

    public List<String> getPeerIds() {
        var selectedProjectFilter = this.getSearchState().getValue().project;
        List<String> peersIds = new ArrayList<>();
        try {
            radPatchesCountDown.await();
        } catch (Exception e) {
            logger.warn("Unable to get rad patches", e);
            return peersIds;
        }
        for (var p : radPatches) {
            if (selectedProjectFilter != null && !p.repo.getRoot().getName().equals(selectedProjectFilter)) {
                continue;
            }
            if (!peersIds.contains(p.peerId)) {
                peersIds.add(p.peerId);
            }
        }
        return peersIds;
    }

    @NotNull
    @Override
    protected PatchListSearchValue withQuery(@NotNull PatchListSearchValue patchListSearchValue, @Nullable String searchStr) {
        var copyPatchSearchValue = new PatchListSearchValue(patchListSearchValue);
        copyPatchSearchValue.searchQuery = searchStr;
        return copyPatchSearchValue;
    }

    @Override
    public List<PatchListQuickFilter> getQuickFilters() {
        return List.of();
    }


    public static class PatchListQuickFilter implements ReviewListQuickFilter<PatchListSearchValue> {

        private PatchListSearchValue patchListSearchValue;

        public PatchListQuickFilter() {
            patchListSearchValue = new PatchListSearchValue();
        }

        @NotNull
        @Override
        public PatchListSearchValue getFilter() {
            return patchListSearchValue;
        }
    }

}
