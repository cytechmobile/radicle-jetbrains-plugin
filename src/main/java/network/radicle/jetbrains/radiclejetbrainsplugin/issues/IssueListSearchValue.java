package network.radicle.jetbrains.radiclejetbrainsplugin.issues;
import network.radicle.jetbrains.radiclejetbrainsplugin.toolwindow.AbstractReviewListSearchValue;

public class IssueListSearchValue extends AbstractReviewListSearchValue {

    public IssueListSearchValue(IssueListSearchValue searchValue) {
        super(searchValue.project, searchValue.searchQuery, searchValue.author,
                searchValue.state, searchValue.tag);
    }

    public IssueListSearchValue() {

    }
}
