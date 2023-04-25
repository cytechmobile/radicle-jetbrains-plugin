package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public class PatchListSearchValue implements ReviewListSearchValue {
    public String project;
    public String searchQuery;
    public String author;

    public String getProject() {
        return project;
    }
    public String getAuthor() {
        return author;
    }

    public PatchListSearchValue() {
    }

    public PatchListSearchValue(PatchListSearchValue searchValue) {
        this.project = searchValue.project;
        this.searchQuery = searchValue.searchQuery;
        this.author = searchValue.author;
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
        return count;
    }

    @Nullable
    @Override
    public String getSearchQuery() {
        return searchQuery;
    }
}
