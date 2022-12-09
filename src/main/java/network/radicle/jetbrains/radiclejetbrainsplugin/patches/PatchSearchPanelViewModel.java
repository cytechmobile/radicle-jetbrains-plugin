package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchSearchPanelViewModel extends ReviewListSearchPanelViewModelBase<PatchListSearchValue,
        PatchSearchPanelViewModel.PatchListQuickFilter> {

    public PatchSearchPanelViewModel(@NotNull CoroutineScope scope,
                                     @NotNull ReviewListSearchHistoryModel<PatchListSearchValue> historyModel, @NotNull
                                     PatchListSearchValue emptySearch, @NotNull PatchSearchPanelViewModel.PatchListQuickFilter defaultQuickFilter) {
        super(scope, historyModel, emptySearch, defaultQuickFilter);
    }

    /*
    public MutableStateFlow<Object> test() {
        var searchState = getSearchState();
        var k = partialState(searchState, new Function1<PatchListSearchValue, Object>() {
            @Override
            public Object invoke(PatchListSearchValue patchListSearchValue) {
                System.out.println("invoke");
                return null;
            }
        }, new Function2<PatchListSearchValue, Object, PatchListSearchValue>() {
            @Override
            public PatchListSearchValue invoke(PatchListSearchValue patchListSearchValue, Object o) {
                System.out.println("getter:" + (String) o);
                patchListSearchValue.state = (String) o;
                return patchListSearchValue;
            }
        });
        return k;
    } */

    public MutableStateFlow<Object> test1() {
        var searchState = getSearchState();
        var k = partialState(searchState, new Function1<PatchListSearchValue, Object>() {
            @Override
            public Object invoke(PatchListSearchValue patchListSearchValue) {
                System.out.println("invoke1");
                return null;
            }
        }, new Function2<PatchListSearchValue, Object, PatchListSearchValue>() {
            @Override
            public PatchListSearchValue invoke(PatchListSearchValue patchListSearchValue, Object o) {
                patchListSearchValue.author = (String) o;
                System.out.println("getter1:" + (String) o);
                return patchListSearchValue;
            }
        });
        return k;
    }

    @NotNull
    @Override
    public List<PatchListQuickFilter> getQuickFilters() {
        var t = new PatchListQuickFilter();
        return List.of();
    }

    @NotNull
    @Override
    protected PatchListSearchValue withQuery(@NotNull PatchListSearchValue patchListSearchValue, @Nullable String s1) {
        System.out.println("withQuery");
        patchListSearchValue.searchQuery = s1;
        return patchListSearchValue;
    }

    public static class PatchListQuickFilter implements ReviewListQuickFilter<PatchListSearchValue> {

        @NotNull
        @Override
        public PatchListSearchValue getFilter() {
            var p = new PatchListSearchValue();
            p.author = "stelios";
            return p;
        }
    }
}
