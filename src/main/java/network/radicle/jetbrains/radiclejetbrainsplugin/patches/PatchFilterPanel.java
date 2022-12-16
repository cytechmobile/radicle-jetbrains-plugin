package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.PopupItemPresentation;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;

import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PatchFilterPanel extends ReviewListSearchPanelFactory<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilter,PatchSearchPanelViewModel> {
    private final PatchSearchPanelViewModel viewModel;
    public PatchFilterPanel(@NotNull PatchSearchPanelViewModel patchSearchPanelViewModel) {
        super(patchSearchPanelViewModel);
        this.viewModel = patchSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var stateFilter = new DropDownComponentFactory<>
                (this.viewModel.stateFilterState()).create(coroutineScope, RadicleBundle.message("state"),
                Arrays.stream(PatchListSearchValue.State.values()).map(e -> e.name).collect(Collectors.toList()), o -> o);

        var projectFilter = new DropDownComponentFactory<>
                (this.viewModel.projectFilterState()).create(coroutineScope, "Project", o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> this.viewModel.getProjectNames(), projectName ->
                                new PopupItemPresentation.Simple((String) projectName,null,null),continuation));
        return List.of(stateFilter,projectFilter);
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull PatchSearchPanelViewModel.PatchListQuickFilter patchListQuickFilter) {
        return "";
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull PatchListSearchValue patchListSearchValue) {
        return "";
    }
}
