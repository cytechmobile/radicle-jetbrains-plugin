package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String project;
    public String searchQuery;
    public String peerId;

    public String getProject() {
        return project;
    }
    public String getPeerId() {
        return peerId;
    }

    public PatchListSearchValue() {
    }

    public PatchListSearchValue(PatchListSearchValue searchValue) {
        this.project = searchValue.project;
        this.searchQuery = searchValue.searchQuery;
        this.peerId = searchValue.peerId;
    }

    @Override
    public int getFilterCount() {
        int count = 0;
        if (project != null) {
            count++;
        }
        if (searchQuery != null) {
            count++;
        }
        if (peerId != null) {
            count++;
        }
        return count;
    }

    @Nullable
    @Override
    public String getSearchQuery() {
        return searchQuery;
    }
}
