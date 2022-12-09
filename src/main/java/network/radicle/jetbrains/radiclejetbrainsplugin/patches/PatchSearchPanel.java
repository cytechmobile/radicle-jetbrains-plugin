package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.*;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class PatchSearchPanel extends ReviewListSearchPanelFactory<PatchListSearchValue, PatchSearchPanelViewModel.PatchListQuickFilter,PatchSearchPanelViewModel> {

    private PatchSearchPanelViewModel viewModel;
    public PatchSearchPanel(@NotNull PatchSearchPanelViewModel patchSearchPanelViewModel) {
        super(patchSearchPanelViewModel);
        this.viewModel = patchSearchPanelViewModel;
    }

    @NotNull
    @Override
    protected List<JComponent> createFilters(@NotNull CoroutineScope coroutineScope) {
        var states = List.of("Open","Closed");
       // var stateFilter = new DropDownComponentFactory
       //         (this.viewModel.test()).create(coroutineScope, "State", states, o -> o);


        var authors = List.of("stelios","jchirst");
        var authorFilter = new DropDownComponentFactory
                (this.viewModel.test1()).create(coroutineScope, "Authors", authors, o -> o);
        return List.of(authorFilter);
    }

    @NotNull
    @Override
    protected String getQuickFilterTitle(@NotNull PatchSearchPanelViewModel.PatchListQuickFilter patchListQuickFilter) {
        System.out.println("getQuickFilterTitle");
        return "getQuickFilterTitle";
    }

    @NotNull
    @Override
    protected String getShortText(@NotNull PatchListSearchValue patchListSearchValue) {
        System.out.println("shortText");
        return "shortText";
    }
}
