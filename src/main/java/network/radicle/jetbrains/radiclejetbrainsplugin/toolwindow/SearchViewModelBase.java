package network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class SearchViewModelBase<T extends ReviewListSearchValue, E extends ReviewListQuickFilter<T>, Q>
        extends ReviewListSearchPanelViewModelBase<T, E> {

    protected CountDownLatch countDown;
    protected List<Q> myList;

    public SearchViewModelBase(@NotNull CoroutineScope scope, @NotNull ReviewListSearchHistoryModel<T> historyModel,
                               @NotNull T emptySearch, @NotNull E defaultQuickFilter) {
        super(scope, historyModel, emptySearch, defaultQuickFilter);
    }

    public void setList(List<Q> list) {
        this.myList = list;
    }

    public void setCountDown(CountDownLatch countdown) {
        this.countDown = countdown;
    }

    public ReviewListSearchValue getValue() {
        return this.getSearchState().getValue();
    }
}