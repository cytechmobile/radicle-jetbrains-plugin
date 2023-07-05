package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.models.RadIssue;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.Utils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class IssueFilterPanel extends ReviewListSearchPanelFactory<IssueListSearchValue,
        IssueSearchPanelViewModel.IssueListQuickFilter, IssueSearchPanelViewModel> {

    private static final Logger logger = LoggerFactory.getLogger(IssueFilterPanel.class);
    private final IssueSearchPanelViewModel viewModel;
    public IssueFilterPanel(@NotNull IssueSearchPanelViewModel issueSearchPanelViewModel) {
        super(issueSearchPanelViewModel);
        this.viewModel = issueSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var stateFilter = new DropDownComponentFactory<>(this.viewModel.stateFilter()).create(coroutineScope, RadicleBundle.message("state"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> ChooserPopupUtil.INSTANCE.showAsyncChooserPopup(relativePoint, jbPopupPopupState,
                        continuation1 -> Arrays.stream(RadIssue.State.values()).map(e -> e.status).collect(Collectors.toList()), state ->
                                new ChooserPopupUtil.PopupItemPresentation.Simple((String) state, null, null), continuation));

        var projectFilter = new DropDownComponentFactory<>(this.viewModel.projectFilterState()).create(coroutineScope, RadicleBundle.message("project"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.createPopup(viewModel.getProjectNames(), viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var authorFilter = new DropDownComponentFactory<>(this.viewModel.authorFilterState()).create(coroutineScope, RadicleBundle.message("author"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.createPopup(viewModel.getAuthors(), viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var tagFilter = new DropDownComponentFactory<>(this.viewModel.tagFilter()).create(coroutineScope, RadicleBundle.message("tag"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.createPopup(viewModel.getTags(), viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });

        var assigneesFilter = new DropDownComponentFactory<>(this.viewModel.assigneeFilter()).create(coroutineScope, RadicleBundle.message("assignees"), o -> o,
                (relativePoint, jbPopupPopupState, continuation) -> {
                    var popUp = Utils.createPopup(viewModel.getAssignees(), viewModel.getCountDown());
                    return ChooserPopupUtil.INSTANCE.showAndAwaitListSubmission(popUp, relativePoint, continuation);
                });


        return List.of(stateFilter, projectFilter, authorFilter, assigneesFilter, tagFilter);
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull IssueListSearchValue issueListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull IssueSearchPanelViewModel.IssueListQuickFilter issueListQuickFilter) {
        return Objects.requireNonNullElse(issueListQuickFilter.getFilter().state, "");
    }
}
