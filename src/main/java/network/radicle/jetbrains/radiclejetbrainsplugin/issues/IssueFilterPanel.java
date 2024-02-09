package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import network.radicle.jetbrains.radiclejetbrainsplugin.RadicleBundle;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.DropdownFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.util.List;
import java.util.Objects;

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
        labelFilter.setShowPopupAction(() -> labelFilter.showPopup(viewModel.getLabels(), this.viewModel.getCountDown()));

        var assigneesFilter = new DropdownFilter(RadicleBundle.message("assignees"),
                this.viewModel.assigneeFilter(), coroutineScope.getCoroutineContext());
        assigneesFilter.setShowPopupAction(() -> assigneesFilter.showPopup(viewModel.getAssignees(), this.viewModel.getCountDown()));

        return List.of(stateFilter.init(), projectFilter.init(), authorFilter.init(), assigneesFilter.init(), labelFilter.init());
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
