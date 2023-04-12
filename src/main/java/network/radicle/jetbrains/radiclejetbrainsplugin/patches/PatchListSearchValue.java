package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String project;
    public String searchQuery;
    public String author;
    public String state;
    public String tag;

    public String getProject() {
        return project;
    }
    public String getAuthor() {
        return author;
    }

    public String getTag() {
        return tag;
    }
    public String getState() {
        return state;
    }

    public PatchListSearchValue() {
    }

    public PatchListSearchValue(PatchListSearchValue searchValue) {
        this.project = searchValue.project;
        this.searchQuery = searchValue.searchQuery;
        this.author = searchValue.author;
        this.state = searchValue.state;
        this.tag = searchValue.tag;
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
        if (author != null) {
            count++;
        }
        if (state != null) {
            count++;
        }
        if (tag != null) {
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
