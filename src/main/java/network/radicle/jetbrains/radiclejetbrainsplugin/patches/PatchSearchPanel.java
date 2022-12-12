package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlin.jvm.functions.Function1;

import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PatchSearchPanel extends ReviewListSearchPanelFactory<PatchListSearchValue, PatchSearchPanelViewModel.PatchListQuickFilter,PatchSearchPanelViewModel> {
    private final PatchSearchPanelViewModel viewModel;
    public PatchSearchPanel(@NotNull PatchSearchPanelViewModel patchSearchPanelViewModel) {
        super(patchSearchPanelViewModel);
        this.viewModel = patchSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var stateFilter = new DropDownComponentFactory<>
                (this.viewModel.stateFilterState()).create(coroutineScope, RadicleBundle.message("state"),
                Arrays.stream(PatchListSearchValue.State.values()).map(e -> e.name).collect(Collectors.toList()), o -> o);
        return List.of(stateFilter);
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
