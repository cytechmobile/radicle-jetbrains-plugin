package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatchSearchHistoryModel implements ReviewListSearchHistoryModel<PatchListSearchValue> {
    @Nullable
    @Override
    public PatchListSearchValue getLastFilter() {
        return null;
    }

    @Override
    public void setLastFilter(@Nullable PatchListSearchValue patchListSearchValue) {

    }

    @Override
    public void add(@NotNull PatchListSearchValue patchListSearchValue) {

    }

    @NotNull
    @Override
    public List<PatchListSearchValue> getHistory() {
        return null;
    }
}
