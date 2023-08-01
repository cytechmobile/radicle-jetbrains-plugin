package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil.PopupItemPresentation;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadPatch;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        var stateFilter = new DropDownComponentFactory<>(this.viewModel.stateFilter()).create(coroutineScope, RadicleBundle.message("state"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> Arrays.stream(RadPatch.State.values()).map(e -> e.status).collect(Collectors.toList()), state ->
                                new PopupItemPresentation.Simple((String) state, null, null), continuation));

        var projectFilter = new DropDownComponentFactory<>(this.viewModel.projectFilterState()).create(coroutineScope, RadicleBundle.message("project"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.PopupBuilder.createPopup(this.viewModel.getProjectNames(), this.viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var authorFilter = new DropDownComponentFactory<>(this.viewModel.authorFilterState()).create(coroutineScope, RadicleBundle.message("author"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.PopupBuilder.createPopup(this.viewModel.getAuthors(), this.viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var tagFilter = new DropDownComponentFactory<>(this.viewModel.tagFilter()).create(coroutineScope, RadicleBundle.message("tag"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.PopupBuilder.createPopup(this.viewModel.getTags(), this.viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        return List.of(stateFilter, projectFilter, authorFilter, tagFilter);
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
