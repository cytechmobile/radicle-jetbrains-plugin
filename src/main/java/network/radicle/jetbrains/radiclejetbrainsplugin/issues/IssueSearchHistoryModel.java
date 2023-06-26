package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IssueSearchHistoryModel  implements ReviewListSearchHistoryModel<IssueListSearchValue> {
    @Nullable
    @Override
    public IssueListSearchValue getLastFilter() {
        return null;
    }

    @Override
    public void setLastFilter(@Nullable IssueListSearchValue searchValue) {

    }

    @Override
    public void add(@NotNull IssueListSearchValue searchValue) {

    }

    @NotNull
    @Override
    public List<IssueListSearchValue> getHistory() {
        return List.of();
    }
}
