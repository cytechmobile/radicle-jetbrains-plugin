package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.PopupItemPresentation;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.PopupState;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PatchFilterPanel extends ReviewListSearchPanelFactory<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilterr,PatchSearchPanelViewModel> {
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
                (this.viewModel.projectFilterState()).create(coroutineScope, RadicleBundle.message("project"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> this.viewModel.getProjectNames(), projectName ->
                                new PopupItemPresentation.Simple((String) projectName,null,null),continuation));
        return List.of(stateFilter,projectFilter);
    }


    @NotNull
    @Override
    protected String getShortText(@NotNull PatchListSearchValue patchListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull PatchSearchPanelViewModel.PatchListQuickFilterr patchListQuickFilterr) {
        if (!Strings.isNullOrEmpty(patchListQuickFilterr.getFilter().state)) {
            return patchListQuickFilterr.getFilter().state;
        }
        return "";
    }
}
