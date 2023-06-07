package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;

public class IssueFilterPanel extends ReviewListSearchPanelFactory<IssueListSearchValue,
        IssueSearchPanelViewModel.IssueListQuickFilter, IssueSearchPanelViewModel> {
    public IssueFilterPanel(@NotNull IssueSearchPanelViewModel issueSearchPanelViewModel) {
        super(issueSearchPanelViewModel);
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        return List.of();
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull IssueListSearchValue issueListSearchValue) {
        return "";
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull IssueSearchPanelViewModel.IssueListQuickFilter issueListQuickFilter) {
        return "";
    }
}
