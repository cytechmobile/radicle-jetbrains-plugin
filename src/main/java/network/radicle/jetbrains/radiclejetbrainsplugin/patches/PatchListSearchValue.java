package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String state;

    public String project;
    public String searchQuery;
    public String peerId;

    public String getState() {
        return state;
    }

    public String getProject() {
        return project;
    }
    public String getPeerId() {
        return peerId;
    }

    public PatchListSearchValue() {
    }

    public PatchListSearchValue(PatchListSearchValue searchValue) {
        this.state = searchValue.state;
        this.project = searchValue.project;
        this.searchQuery = searchValue.searchQuery;
        this.peerId = searchValue.peerId;
    }

    @Override
    public int getFilterCount() {
        int count = 0;
        if (state != null) {
            count++;
        }
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

    public enum State {
        OPEN("Open"),
        CLOSED("Closed"),
        MERGED("Merged");

        public String name;
        State(String name) {
            this.name = name;
        }
    }

}
