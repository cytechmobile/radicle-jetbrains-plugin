package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractReviewListSearchValue implements ReviewListSearchValue {

    public String project;
    public String searchQuery;
    public String author;
    public String state;
    public String label;

    public AbstractReviewListSearchValue(String project, String searchQuery, String author, String state, String label) {
        this.project = project;
        this.searchQuery = searchQuery;
        this.author = author;
        this.state = state;
        this.label = label;
    }

    public AbstractReviewListSearchValue() {
    }

    public String getProject() {
        return project;
    }

    public String getAuthor() {
        return author;
    }

    public String getState() {
        return state;
    }

    public String getLabel() {
        return label;
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
        if (label != null) {
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
