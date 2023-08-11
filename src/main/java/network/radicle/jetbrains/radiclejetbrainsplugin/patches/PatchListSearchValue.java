package network.radicle.jetbrains.radiclejetbrainsplugin.patches;

import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.AbstractReviewListSearchValue;

public class PatchListSearchValue extends AbstractReviewListSearchValue {

    public PatchListSearchValue(PatchListSearchValue searchValue) {
        super(searchValue.project, searchValue.searchQuery, searchValue.author, searchValue.state, searchValue.label);
    }

    public PatchListSearchValue() {

    }
}
