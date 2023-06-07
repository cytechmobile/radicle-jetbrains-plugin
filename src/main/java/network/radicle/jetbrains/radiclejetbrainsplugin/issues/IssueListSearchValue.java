package network.radicle.jetbrains.radiclejetbrainsplugin.issues;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public class IssueListSearchValue implements ReviewListSearchValue {
    @Nullable
    @Override
    public String getSearchQuery() {
        return "";
    }
}
