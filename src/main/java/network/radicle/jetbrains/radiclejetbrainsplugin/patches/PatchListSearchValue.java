package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String state;

    public String project;
    public String searchQuery;
    public String author;

    public String getState() {
        return state;
    }

    public String getProject() {
        return project;
    }
    public String getAuthor() {
        return author;
    }

    public PatchListSearchValue() {
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
        if (author != null) {
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
