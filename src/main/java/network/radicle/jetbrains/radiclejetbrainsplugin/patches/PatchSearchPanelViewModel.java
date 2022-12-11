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
                                     @NotNull ReviewListSearchHistoryModel<PatchListSearchValue> historyModel) {
        super(scope, historyModel, new PatchListSearchValue(), new PatchListQuickFilter());
    }

    public MutableStateFlow<String> test1() {
        var searchState = getSearchState();
        var k = partialState(searchState, PatchListSearchValue::getAuthor, new Function2<PatchListSearchValue, Object, PatchListSearchValue>() {
            @Override
            public PatchListSearchValue invoke(PatchListSearchValue patchListSearchValue, Object o) {
                var k = new PatchListSearchValue();
                k.author = (String) o;
                System.out.println("getter1:" + (String) o);
                return k;
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
            System.out.println("get Filter");
            return  new PatchListSearchValue();
        }
    }
}
