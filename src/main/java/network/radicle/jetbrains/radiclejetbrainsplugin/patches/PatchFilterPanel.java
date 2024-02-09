package network.radicle.jetbrains.radiclejetbrainsplugin.patches;


import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DropdownFilter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;
import java.util.Objects;

public class PatchFilterPanel extends ReviewListSearchPanelFactory<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilter, PatchSearchPanelViewModel> {
    private final PatchSearchPanelViewModel viewModel;

    public PatchFilterPanel(@NotNull PatchSearchPanelViewModel patchSearchPanelViewModel) {
        super(patchSearchPanelViewModel);
        this.viewModel = patchSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var stateFilter = new DropdownFilter(RadicleBundle.message("state"),
                this.viewModel.stateFilter(), coroutineScope.getCoroutineContext());
        stateFilter.setShowPopupAction(() -> stateFilter.showPopup(this.viewModel.getStateLabels(), this.viewModel.getCountDown()));

        var projectFilter = new DropdownFilter(RadicleBundle.message("project"),
                this.viewModel.projectFilterState(), coroutineScope.getCoroutineContext());
        projectFilter.setShowPopupAction(() -> projectFilter.showPopup(this.viewModel.getProjectNames(), this.viewModel.getCountDown()));

        var authorFilter = new DropdownFilter(RadicleBundle.message("author"),
                this.viewModel.authorFilterState(), coroutineScope.getCoroutineContext());
        authorFilter.setShowPopupAction(() -> authorFilter.showPopup(this.viewModel.getAuthors(), this.viewModel.getCountDown()));

        var labelFilter = new DropdownFilter(RadicleBundle.message("label"),
                this.viewModel.labelFilter(), coroutineScope.getCoroutineContext());
        labelFilter.setShowPopupAction(() -> labelFilter.showPopup(this.viewModel.getLabels(), this.viewModel.getCountDown()));

        return List.of(stateFilter.init(), projectFilter.init(), authorFilter.init(), labelFilter.init());
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull PatchListSearchValue patchListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull PatchSearchPanelViewModel.PatchListQuickFilter patchListQuickFilter) {
        return Objects.requireNonNullElse(patchListQuickFilter.getFilter().state, "");
    }
}
