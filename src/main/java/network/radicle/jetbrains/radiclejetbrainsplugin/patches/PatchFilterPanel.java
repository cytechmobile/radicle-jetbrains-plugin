package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.PopupItemPresentation;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

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
        var projectFilter = new DropDownComponentFactory<>(this.viewModel.projectFilterState()).create(coroutineScope, RadicleBundle.message("project"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> this.viewModel.getProjectNames(), projectName ->
                                new PopupItemPresentation.Simple((String) projectName, null, null), continuation));

        var authorFilter = new DropDownComponentFactory<>(this.viewModel.peerIdFilterState()).create(coroutineScope, RadicleBundle.message("peerIds"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> this.viewModel.getPeerIds(), projectName ->
                                new PopupItemPresentation.Simple((String) projectName, null, null), continuation));

        return List.of(projectFilter, authorFilter);
    }


    @NotNull
    @Override
    protected String getShortText(@NotNull PatchListSearchValue patchListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull PatchSearchPanelViewModel.PatchListQuickFilter patchListQuickFilter) {
        return "";
    }
}
