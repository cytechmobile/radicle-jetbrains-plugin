package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.google.common.base.Strings;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import org.jetbrains.annotations.Nullable;

public class PatchListSearchValue implements ReviewListSearchValue {


    public String state;
    public String author;
    public String searchQuery;

    public String getAuthor() {
        System.out.println("myauthr: " + author);
        return author;
    }

    PatchListSearchValue() {
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

}
