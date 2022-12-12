package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String state;
    public String author;
    public String searchQuery;

    public String getAuthor() {
        return author;
    }

    public String getState() {
        return state;
    }

    public PatchListSearchValue() {
    }

    @Override
    public int getFilterCount() {
        int count = 0;
        if (state != null) {
            count++;
        }
        if (author != null) {
            count++;
        }
        if (searchQuery != null) {
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
